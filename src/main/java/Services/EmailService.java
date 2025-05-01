package services;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    public static final String ADMIN_EMAIL = "sourournajjar2@gmail.com";
    private static EmailService instance;
    private final String senderEmail;
    private final String senderPassword;

    private EmailService() {
        // Configuration pour Gmail
        this.senderEmail = "sourournajjar2@gmail.com"; // Utilisez l'adresse email spécifiée
        this.senderPassword = "runa hfvh forx yjuz"; // Remplacez par le mot de passe d'application
    }

    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    /**
     * Envoie un email de notification pour une nouvelle réclamation
     * @param reclamationType Le type de réclamation
     * @param description La description de la réclamation
     * @param medecin Le nom du médecin concerné
     * @param date La date de la réclamation
     * @return true si l'envoi a réussi, false sinon
     */
    public boolean sendReclamationNotification(String reclamationType, String description, String medecin, String date) {
        String subject = "Nouvelle réclamation: " + reclamationType;

        StringBuilder messageContent = new StringBuilder();
        messageContent.append("Une nouvelle réclamation a été ajoutée dans le système SAHATECK.\n\n");
        messageContent.append("Détails de la réclamation:\n");
        messageContent.append("---------------------------\n");
        messageContent.append("Type: ").append(reclamationType).append("\n");
        messageContent.append("Médecin concerné: ").append(medecin).append("\n");
        messageContent.append("Date: ").append(date).append("\n");
        messageContent.append("Description: ").append(description).append("\n\n");
        messageContent.append("Veuillez consulter l'application SAHATECK pour traiter cette réclamation.");

        return sendEmail(ADMIN_EMAIL, subject, messageContent.toString());
    }

    /**
     * Méthode générale pour envoyer un email
     * @param recipientEmail L'adresse email du destinataire
     * @param subject Le sujet de l'email
     * @param messageContent Le contenu de l'email
     * @return true si l'envoi a réussi, false sinon
     */
    public boolean sendEmail(String recipientEmail, String subject, String messageContent) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        try {
            // Créer une session avec l'authentification
            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, senderPassword);
                }
            });

            // Créer le message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(messageContent);

            // Envoyer le message
            Transport.send(message);
            LOGGER.log(Level.INFO, "Email envoyé avec succès à {0}", recipientEmail);
            return true;
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'envoi de l'email: {0}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}