package service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import model.Evenement;
import model.Participant;
import util.ConfigManager;

/**
 * Service pour l'envoi d'emails aux participants
 */
public class MailService {
    
    private final String username;
    private final String password;
    private final String smtpHost;
    private final int smtpPort;
    private final boolean smtpAuth;
    private final boolean smtpStartTlsEnable;
    private final String senderEmail;
    private final String senderName;
    private final boolean enabled;
    
    /**
     * Constructeur qui initialise les paramètres SMTP à partir du fichier de configuration
     */
    public MailService() {
        ConfigManager configManager = ConfigManager.getInstance();
        
        this.username = configManager.getProperty("mail.smtp.username");
        this.password = configManager.getProperty("mail.smtp.password");
        this.smtpHost = configManager.getProperty("mail.smtp.host");
        this.smtpPort = Integer.parseInt(configManager.getProperty("mail.smtp.port", "587"));
        this.smtpAuth = Boolean.parseBoolean(configManager.getProperty("mail.smtp.auth", "true"));
        this.smtpStartTlsEnable = Boolean.parseBoolean(configManager.getProperty("mail.smtp.starttls.enable", "true"));
        this.senderEmail = configManager.getProperty("mail.sender.email");
        this.senderName = configManager.getProperty("mail.sender.name", "Gestion d'Événements");
        this.enabled = Boolean.parseBoolean(configManager.getProperty("mail.enabled", "true"));
    }
    
    /**
     * Crée une session SMTP
     * 
     * @return Session SMTP configurée
     */
    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtpStartTlsEnable));
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }
    
    /**
     * Envoie un email
     * 
     * @param to Adresse email du destinataire
     * @param subject Sujet de l'email
     * @param content Contenu HTML de l'email
     * @return true si l'email a été envoyé avec succès
     */
    public boolean sendEmail(String to, String subject, String content) {
        if (!enabled) {
            System.out.println("L'envoi d'emails est désactivé dans la configuration");
            return false;
        }
        
        try {
            Session session = createSession();
            
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, senderName));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject, "UTF-8");
            message.setContent(content, "text/html; charset=UTF-8");
            message.setSentDate(new Date());
            
            Transport.send(message);
            return true;
        } catch (MessagingException | IOException e) {
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Envoie un email de confirmation d'inscription à un événement
     * 
     * @param participant Participant inscrit
     * @param event Événement concerné
     * @return true si l'email a été envoyé avec succès
     */
    public boolean sendRegistrationConfirmation(Participant participant, Evenement event) {
        String subject = "Confirmation d'inscription - " + event.getTitre();
        
        // Construire le contenu HTML avec un template simple
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        content.append("<style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;max-width:600px;margin:0 auto;padding:20px} ");
        content.append("h1{color:#2c3e50;} .highlight{color:#3498db;font-weight:bold} ");
        content.append(".details{background-color:#f8f9fa;border-left:4px solid #3498db;padding:15px;margin:20px 0} ");
        content.append(".footer{margin-top:30px;font-size:0.9em;color:#7f8c8d;border-top:1px solid #eee;padding-top:20px}</style>");
        content.append("</head><body>");
        
        content.append("<h1>Confirmation d'inscription</h1>");
        content.append("<p>Bonjour <span class='highlight'>").append(participant.getPrenom()).append(" ").append(participant.getNom()).append("</span>,</p>");
        content.append("<p>Nous vous confirmons votre inscription à l'événement suivant :</p>");
        
        content.append("<div class='details'>");
        content.append("<p><strong>Événement : </strong>").append(event.getTitre()).append("</p>");
        content.append("<p><strong>Date : </strong>").append(event.getDate()).append("</p>");
        content.append("<p><strong>Lieu : </strong>").append(event.getLieu()).append("</p>");
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            content.append("<p><strong>Description : </strong>").append(event.getDescription()).append("</p>");
        }
        content.append("</div>");
        
        content.append("<p>Merci de votre inscription. Pour toute question, n'hésitez pas à nous contacter.</p>");
        
        // Ajouter des informations météo si disponible
        try {
            WeatherService weatherService = new WeatherService();
            String weatherInfo = weatherService.getWeatherRecommendation(event.getLieu(), event.getDate());
            if (weatherInfo != null && !weatherInfo.isEmpty()) {
                content.append("<div class='details'>");
                content.append("<p><strong>Prévisions météo : </strong>").append(weatherInfo).append("</p>");
                content.append("</div>");
            }
        } catch (Exception e) {
            // Ignorer silencieusement les erreurs de météo
        }
        
        content.append("<div class='footer'>");
        content.append("<p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>");
        content.append("</div>");
        content.append("</body></html>");
        
        return sendEmail(participant.getEmail(), subject, content.toString());
    }
    
    /**
     * Envoie un email d'annulation d'événement
     * 
     * @param event Événement annulé
     * @param participants Liste des participants inscrits
     * @param reason Raison de l'annulation
     * @return Nombre d'emails envoyés avec succès
     */
    public int sendCancellationNotification(Evenement event, List<Participant> participants, String reason) {
        if (participants == null || participants.isEmpty()) {
            return 0;
        }
        
        String subject = "Annulation de l'événement - " + event.getTitre();
        int successCount = 0;
        
        for (Participant participant : participants) {
            // Construire le contenu HTML avec un template simple
            StringBuilder content = new StringBuilder();
            content.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
            content.append("<style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;max-width:600px;margin:0 auto;padding:20px} ");
            content.append("h1{color:#c0392b;} .highlight{color:#e74c3c;font-weight:bold} ");
            content.append(".details{background-color:#f8f9fa;border-left:4px solid #e74c3c;padding:15px;margin:20px 0} ");
            content.append(".footer{margin-top:30px;font-size:0.9em;color:#7f8c8d;border-top:1px solid #eee;padding-top:20px}</style>");
            content.append("</head><body>");
            
            content.append("<h1>Annulation d'événement</h1>");
            content.append("<p>Bonjour <span class='highlight'>").append(participant.getPrenom()).append(" ").append(participant.getNom()).append("</span>,</p>");
            content.append("<p>Nous sommes désolés de vous informer que l'événement suivant a été <strong>annulé</strong> :</p>");
            
            content.append("<div class='details'>");
            content.append("<p><strong>Événement : </strong>").append(event.getTitre()).append("</p>");
            content.append("<p><strong>Date : </strong>").append(event.getDate()).append("</p>");
            content.append("<p><strong>Lieu : </strong>").append(event.getLieu()).append("</p>");
            
            if (reason != null && !reason.isEmpty()) {
                content.append("<p><strong>Raison de l'annulation : </strong>").append(reason).append("</p>");
            }
            content.append("</div>");
            
            content.append("<p>Nous vous prions de nous excuser pour tout désagrément que cela pourrait causer.</p>");
            content.append("<p>Notre équipe vous contactera prochainement pour vous proposer d'autres événements similaires.</p>");
            
            content.append("<div class='footer'>");
            content.append("<p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>");
            content.append("</div>");
            content.append("</body></html>");
            
            if (sendEmail(participant.getEmail(), subject, content.toString())) {
                successCount++;
            }
        }
        
        return successCount;
    }
    
    /**
     * Envoie un email de notification pour promotion depuis la liste d'attente
     * 
     * @param participant Participant promu
     * @param event Événement concerné
     * @return true si l'email a été envoyé avec succès
     */
    public boolean sendWaitingListPromotionNotification(Participant participant, Evenement event) {
        String subject = "Bonne nouvelle ! Vous êtes inscrit à " + event.getTitre();
        
        // Construire le contenu HTML avec un template simple
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        content.append("<style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;max-width:600px;margin:0 auto;padding:20px} ");
        content.append("h1{color:#27ae60;} .highlight{color:#2ecc71;font-weight:bold} ");
        content.append(".details{background-color:#f8f9fa;border-left:4px solid #2ecc71;padding:15px;margin:20px 0} ");
        content.append(".footer{margin-top:30px;font-size:0.9em;color:#7f8c8d;border-top:1px solid #eee;padding-top:20px}</style>");
        content.append("</head><body>");
        
        content.append("<h1>Bonne nouvelle !</h1>");
        content.append("<p>Bonjour <span class='highlight'>").append(participant.getPrenom()).append(" ").append(participant.getNom()).append("</span>,</p>");
        content.append("<p>Nous sommes heureux de vous informer qu'une place s'est libérée pour l'événement suivant :</p>");
        
        content.append("<div class='details'>");
        content.append("<p><strong>Événement : </strong>").append(event.getTitre()).append("</p>");
        content.append("<p><strong>Date : </strong>").append(event.getDate()).append("</p>");
        content.append("<p><strong>Lieu : </strong>").append(event.getLieu()).append("</p>");
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            content.append("<p><strong>Description : </strong>").append(event.getDescription()).append("</p>");
        }
        content.append("</div>");
        
        content.append("<p>Vous aviez été placé sur liste d'attente et nous avons le plaisir de vous confirmer que <strong>votre inscription est maintenant confirmée</strong>.</p>");
        
        // Ajouter des informations météo si disponible
        try {
            WeatherService weatherService = new WeatherService();
            String weatherInfo = weatherService.getWeatherRecommendation(event.getLieu(), event.getDate());
            if (weatherInfo != null && !weatherInfo.isEmpty()) {
                content.append("<div class='details'>");
                content.append("<p><strong>Prévisions météo : </strong>").append(weatherInfo).append("</p>");
                content.append("</div>");
            }
        } catch (Exception e) {
            // Ignorer silencieusement les erreurs de météo
        }
        
        content.append("<div class='footer'>");
        content.append("<p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>");
        content.append("</div>");
        content.append("</body></html>");
        
        return sendEmail(participant.getEmail(), subject, content.toString());
    }
    
    /**
     * Envoie un email de rappel quelques jours avant un événement
     * 
     * @param participant Participant inscrit
     * @param event Événement concerné
     * @param daysRemaining Nombre de jours restants avant l'événement
     * @return true si l'email a été envoyé avec succès
     */
    public boolean sendEventReminder(Participant participant, Evenement event, int daysRemaining) {
        String subject = "Rappel : " + event.getTitre() + " dans " + daysRemaining + " jour(s)";
        
        // Construire le contenu HTML avec un template simple
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        content.append("<style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;max-width:600px;margin:0 auto;padding:20px} ");
        content.append("h1{color:#f39c12;} .highlight{color:#f39c12;font-weight:bold} ");
        content.append(".details{background-color:#f8f9fa;border-left:4px solid #f39c12;padding:15px;margin:20px 0} ");
        content.append(".footer{margin-top:30px;font-size:0.9em;color:#7f8c8d;border-top:1px solid #eee;padding-top:20px}</style>");
        content.append("</head><body>");
        
        content.append("<h1>Rappel d'événement</h1>");
        content.append("<p>Bonjour <span class='highlight'>").append(participant.getPrenom()).append(" ").append(participant.getNom()).append("</span>,</p>");
        content.append("<p>Nous vous rappelons que vous êtes inscrit à l'événement suivant qui aura lieu dans <strong>").append(daysRemaining).append(" jour(s)</strong> :</p>");
        
        content.append("<div class='details'>");
        content.append("<p><strong>Événement : </strong>").append(event.getTitre()).append("</p>");
        content.append("<p><strong>Date : </strong>").append(event.getDate()).append("</p>");
        content.append("<p><strong>Lieu : </strong>").append(event.getLieu()).append("</p>");
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            content.append("<p><strong>Description : </strong>").append(event.getDescription()).append("</p>");
        }
        content.append("</div>");
        
        // Ajouter des informations météo si disponible
        try {
            WeatherService weatherService = new WeatherService();
            String weatherInfo = weatherService.getWeatherRecommendation(event.getLieu(), event.getDate());
            if (weatherInfo != null && !weatherInfo.isEmpty()) {
                content.append("<div class='details'>");
                content.append("<p><strong>Prévisions météo : </strong>").append(weatherInfo).append("</p>");
                content.append("</div>");
            }
        } catch (Exception e) {
            // Ignorer silencieusement les erreurs de météo
        }
        
        content.append("<p>N'hésitez pas à nous contacter si vous avez des questions.</p>");
        content.append("<p>Nous avons hâte de vous voir !</p>");
        
        content.append("<div class='footer'>");
        content.append("<p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>");
        content.append("</div>");
        content.append("</body></html>");
        
        return sendEmail(participant.getEmail(), subject, content.toString());
    }
    
    /**
     * Envoie un email de notification pour changement dans un événement
     * 
     * @param participant Participant inscrit
     * @param event Événement modifié
     * @param changes Description des changements
     * @return true si l'email a été envoyé avec succès
     */
    public boolean sendEventChangesNotification(Participant participant, Evenement event, String changes) {
        String subject = "Changements importants - " + event.getTitre();
        
        // Construire le contenu HTML avec un template simple
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        content.append("<style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;max-width:600px;margin:0 auto;padding:20px} ");
        content.append("h1{color:#9b59b6;} .highlight{color:#9b59b6;font-weight:bold} ");
        content.append(".details{background-color:#f8f9fa;border-left:4px solid #9b59b6;padding:15px;margin:20px 0} ");
        content.append(".changes{background-color:#f8f9fa;border-left:4px solid #e74c3c;padding:15px;margin:20px 0;color:#e74c3c} ");
        content.append(".footer{margin-top:30px;font-size:0.9em;color:#7f8c8d;border-top:1px solid #eee;padding-top:20px}</style>");
        content.append("</head><body>");
        
        content.append("<h1>Notification de changements</h1>");
        content.append("<p>Bonjour <span class='highlight'>").append(participant.getPrenom()).append(" ").append(participant.getNom()).append("</span>,</p>");
        content.append("<p>Nous vous informons que des changements ont été apportés à l'événement suivant auquel vous êtes inscrit :</p>");
        
        content.append("<div class='details'>");
        content.append("<p><strong>Événement : </strong>").append(event.getTitre()).append("</p>");
        content.append("<p><strong>Date : </strong>").append(event.getDate()).append("</p>");
        content.append("<p><strong>Lieu : </strong>").append(event.getLieu()).append("</p>");
        content.append("</div>");
        
        content.append("<div class='changes'>");
        content.append("<p><strong>Changements apportés :</strong></p>");
        content.append("<p>").append(changes).append("</p>");
        content.append("</div>");
        
        content.append("<p>Si ces changements vous posent problème, n'hésitez pas à nous contacter pour annuler votre inscription.</p>");
        
        content.append("<div class='footer'>");
        content.append("<p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>");
        content.append("</div>");
        content.append("</body></html>");
        
        return sendEmail(participant.getEmail(), subject, content.toString());
    }
    
    /**
     * Teste si le service d'email est correctement configuré
     * 
     * @param testEmail Adresse email pour le test
     * @return true si le test est réussi
     */
    public boolean testEmailConfiguration(String testEmail) {
        String subject = "Test de configuration email";
        String content = "<!DOCTYPE html><html><body><h1>Test de configuration email</h1>"
                      + "<p>Si vous recevez cet email, cela signifie que la configuration de votre service d'email fonctionne correctement.</p>"
                      + "</body></html>";
        
        return sendEmail(testEmail, subject, content);
    }
} 