package services;

import models.Event;
import models.User;
import utils.EmailConfig;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.activation.DataHandler;
import javax.activation.DataSource;

/**
 * Service gérant l'envoi d'emails aux utilisateurs
 */
public class EmailService {
    private final Properties properties;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
    
    public EmailService() {
        properties = new Properties();
        
        // Configuration de base
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.host", EmailConfig.SMTP_HOST);
        properties.put("mail.smtp.port", EmailConfig.SMTP_PORT);
        
        // Configuration SSL (port 465) ou TLS (port 587)
        if (EmailConfig.SMTP_PORT.equals("465")) {
            // Pour port 465 - SSL direct
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.socketFactory.port", EmailConfig.SMTP_PORT);
            properties.put("mail.smtp.socketFactory.fallback", "false");
        } else {
            // Pour port 587 - STARTTLS
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
        }
        
        // Configuration SSL supplémentaire
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        properties.put("mail.smtp.ssl.trust", "*");
        
        // Délai d'attente augmenté
        properties.put("mail.smtp.connectiontimeout", "20000");
        properties.put("mail.smtp.timeout", "20000");
        properties.put("mail.smtp.writetimeout", "20000");
        
        // Débogage SMTP
        properties.put("mail.debug", String.valueOf(EmailConfig.DEBUG_ENABLED));
        properties.put("mail.debug.auth", String.valueOf(EmailConfig.DEBUG_ENABLED));
    }
    
    /**
     * Récupère une session JavaMail configurée
     * @return La session JavaMail ou null en cas d'erreur
     */
    private Session getSession() {
        try {
            if (!EmailConfig.isConfigValid()) {
                System.err.println("Configuration email invalide");
                return null;
            }
            
            final String username = EmailConfig.EMAIL_USERNAME;
            final String password = EmailConfig.EMAIL_PASSWORD;
            
            // On ne crée pas de nouvelles Properties pour éviter de perdre la configuration
            
            if (EmailConfig.DEBUG_ENABLED) {
                System.out.println("Configuration email: " + username + " via " + EmailConfig.SMTP_HOST + ":" + EmailConfig.SMTP_PORT);
                System.out.println("Mode SSL: " + properties.getProperty("mail.smtp.ssl.enable", "false"));
                System.out.println("Mode STARTTLS: " + properties.getProperty("mail.smtp.starttls.enable", "false"));
                System.out.println("Socket Factory: " + properties.getProperty("mail.smtp.socketFactory.class", "none"));
                System.out.println("SSL Protocols: " + properties.getProperty("mail.smtp.ssl.protocols", "none"));
            }
            
            return Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la session email: " + e.getMessage());
            if (EmailConfig.DEBUG_ENABLED) {
                e.printStackTrace();
            }
            return null;
        }
    }
    
    /**
     * Envoie un email à l'adresse spécifiée
     * @param to L'adresse email destinataire
     * @param subject Le sujet de l'email
     * @param content Le contenu de l'email (peut être du HTML)
     * @return true si l'email a été envoyé avec succès
     */
    public boolean sendEmail(String to, String subject, String content) {
        if (!EmailConfig.ENABLE_EMAIL) {
            System.out.println("Envoi d'email désactivé dans la configuration.");
            return false;
        }
        
        if (to == null || to.isEmpty() || !to.contains("@")) {
            System.err.println("Adresse email invalide: " + to);
            return false;
        }
        
        Session session = getSession();
        if (session == null) {
            System.err.println("Impossible d'obtenir une session email valide");
            return false;
        }
        
        try {
            MimeMessage message = new MimeMessage(session);
            
            // Configurer l'expéditeur
            message.setFrom(new InternetAddress(EmailConfig.FROM_EMAIL, EmailConfig.FROM_NAME));
            
            // Configurer le destinataire
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            
            // Configurer le sujet
            message.setSubject(subject, "UTF-8");
            
            // Configurer le contenu
            message.setContent(content, "text/html; charset=UTF-8");
            
            // Envoyer l'email
            System.out.println("Tentative d'envoi d'email à: " + to);
            System.out.println("  - Serveur SMTP: " + EmailConfig.SMTP_HOST + ":" + EmailConfig.SMTP_PORT);
            System.out.println("  - Expéditeur: " + EmailConfig.FROM_EMAIL);
            
            Transport.send(message);
            System.out.println("✓ Email envoyé avec succès à: " + to);
            
            return true;
        } catch (MessagingException e) {
            System.err.println("✖ Erreur SMTP lors de l'envoi de l'email: " + e.getMessage());
            
            if (e.getNextException() != null) {
                System.err.println("  Cause racine: " + e.getNextException().getMessage());
            }
            
            if (EmailConfig.DEBUG_ENABLED) {
                System.err.println("Détail de l'erreur:");
                e.printStackTrace();
                
                // Vérifier les erreurs spécifiques aux problèmes de port/SSL
                if (e.getMessage().contains("Could not connect") ||
                    e.getMessage().contains("Connection timed out") ||
                    (e.getNextException() != null && e.getNextException().getMessage().contains("Connection refused"))) {
                    System.err.println("\nSuggestion: Vérifiez que le port " + EmailConfig.SMTP_PORT + 
                                      " est correct et que votre fournisseur permet l'accès via SSL/TLS.");
                }
            }
            
            return false;
        } catch (java.io.UnsupportedEncodingException e) {
            System.err.println("? Erreur d'encodage lors de l'envoi de l'email: " + e.getMessage());
            if (EmailConfig.DEBUG_ENABLED) {
                e.printStackTrace();
            }
            return false;
        } catch (Exception e) {
            System.err.println("! Exception inattendue lors de l'envoi de l'email: " + e.getMessage());
            if (EmailConfig.DEBUG_ENABLED) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Envoie un email avec pièces jointes (PDF et/ou fichiers)
     * @param to Adresse email du destinataire
     * @param subject Sujet de l'email
     * @param content Contenu HTML de l'email
     * @param attachments Tableau d'objets EmailAttachment contenant les pièces jointes
     * @return true si l'envoi a réussi
     */
    public boolean sendEmailWithAttachments(String to, String subject, String content, EmailAttachment[] attachments) {
        if (!EmailConfig.ENABLE_EMAIL) {
            System.out.println("Envoi d'email désactivé dans la configuration.");
            return false;
        }
        
        if (to == null || to.isEmpty() || !to.contains("@")) {
            System.err.println("Adresse email invalide: " + to);
            return false;
        }
        
        Session session = getSession();
        if (session == null) {
            System.err.println("Impossible d'obtenir une session email valide");
            return false;
        }
        
        try {
            // Créer un message avec plusieurs parties
            MimeMessage message = new MimeMessage(session);
            
            // Configurer l'expéditeur et destinataire
            message.setFrom(new InternetAddress(EmailConfig.FROM_EMAIL, EmailConfig.FROM_NAME));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject, "UTF-8");
            
            // Créer la partie multipart
            Multipart multipart = new MimeMultipart();
            
            // Partie 1: Corps du message HTML
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(content, "text/html; charset=UTF-8");
            multipart.addBodyPart(messageBodyPart);
            
            // Ajouter toutes les pièces jointes
            if (attachments != null) {
                for (EmailAttachment attachment : attachments) {
                    if (attachment != null && attachment.getData() != null) {
                        MimeBodyPart attachmentPart = new MimeBodyPart();
                        
                        // Fournir les données de la pièce jointe
                        DataSource source = 
                            new DataSource() {
                                @Override
                                public InputStream getInputStream() throws IOException {
                                    return new ByteArrayInputStream(attachment.getData());
                                }
                                
                                @Override
                                public OutputStream getOutputStream() throws IOException {
                                    throw new IOException("Non supporté");
                                }
                                
                                @Override
                                public String getContentType() {
                                    return attachment.getContentType();
                                }
                                
                                @Override
                                public String getName() {
                                    return attachment.getFilename();
                                }
                            };
                        
                        attachmentPart.setDataHandler(new DataHandler(source));
                        attachmentPart.setFileName(attachment.getFilename());
                        multipart.addBodyPart(attachmentPart);
                    }
                }
            }
            
            // Associer la partie multipart au message
            message.setContent(multipart);
            
            // Envoyer l'email
            System.out.println("Tentative d'envoi d'email avec pièces jointes à: " + to);
            System.out.println("  - Serveur SMTP: " + EmailConfig.SMTP_HOST + ":" + EmailConfig.SMTP_PORT);
            System.out.println("  - Expéditeur: " + EmailConfig.FROM_EMAIL);
            System.out.println("  - Nombre de pièces jointes: " + (attachments != null ? attachments.length : 0));
            
            Transport.send(message);
            System.out.println("✓ Email avec pièces jointes envoyé avec succès à: " + to);
            
            return true;
        } catch (MessagingException e) {
            System.err.println("✖ Erreur SMTP lors de l'envoi de l'email: " + e.getMessage());
            
            if (e.getNextException() != null) {
                System.err.println("  Cause racine: " + e.getNextException().getMessage());
            }
            
            if (EmailConfig.DEBUG_ENABLED) {
                e.printStackTrace();
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("! Exception inattendue lors de l'envoi de l'email: " + e.getMessage());
            if (EmailConfig.DEBUG_ENABLED) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Classe pour représenter une pièce jointe à un email
     */
    public static class EmailAttachment {
        private final String filename;
        private final String contentType;
        private final byte[] data;
        
        public EmailAttachment(String filename, String contentType, byte[] data) {
            this.filename = filename;
            this.contentType = contentType;
            this.data = data;
        }
        
        public String getFilename() {
            return filename;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public byte[] getData() {
            return data;
        }
    }
    
    /**
     * Envoie un email de confirmation d'inscription à un événement avec PDF et QR code
     * @param user Utilisateur qui s'inscrit
     * @param event Événement auquel l'utilisateur s'inscrit
     * @return true si l'envoi a réussi, false sinon
     */
    public boolean sendEventRegistrationConfirmation(User user, Event event) {
        if (user == null || event == null) {
            return false;
        }
        
        // Vérifier que l'utilisateur a une adresse email
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            System.err.println("L'utilisateur n'a pas d'adresse email: " + user.getId());
            return false;
        }
        
        String subject = "Confirmation d'inscription: " + event.getTitle();
        
        String content = 
                "<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;'>" +
                "<h2 style='color: #4568dc;'>Confirmation d'inscription</h2>" +
                "<p>Bonjour <b>" + user.getNom() + " " + user.getPrenom() + "</b>,</p>" +
                "<p>Votre inscription à l'événement suivant a été confirmée :</p>" +
                "<div style='background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 15px 0;'>" +
                "<h3 style='margin-top: 0; color: #333;'>" + event.getTitle() + "</h3>" +
                "<p><b>Date :</b> Du " + event.getStartDate().format(dateFormatter) + " au " + 
                event.getEndDate().format(dateFormatter) + "</p>" +
                "<p><b>Lieu :</b> " + event.getLocation() + "</p>" +
                "<p><b>Description :</b> " + event.getDescription() + "</p>" +
                "</div>" +
                "<p>N'hésitez pas à vous désinscrire si vous ne pouvez plus participer, afin de libérer votre place.</p>" +
                "<p>Nous vous remercions de votre participation et vous souhaitons un excellent événement !</p>" +
                "<p><b>Vous trouverez en pièce jointe :</b></p>" +
                "<ul>" +
                "<li>Un document PDF contenant les détails de l'événement</li>" +
                "<li>Un QR code à présenter le jour de l'événement</li>" +
                "</ul>" +
                "<p>Vous pouvez également consulter ces informations en ligne :</p>" +
                "<div style='text-align: center; margin: 20px 0;'>" +
                "<a href='" + generateEventDetailsUrl(event.getId(), user.getId()) + "' " +
                "style='display: inline-block; background-color: #4568dc; color: white; padding: 10px 20px; " +
                "text-decoration: none; border-radius: 5px; font-weight: bold;'>" +
                "Voir mon billet en ligne</a>" +
                "</div>" +
                "<p style='margin-top: 30px; font-size: 0.9em; color: #777;'>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>" +
                "</div></body></html>";
        
        if (!EmailConfig.isConfigValid()) {
            // En mode simulation, on affiche simplement un message
            System.out.println("=== SIMULATION D'ENVOI D'EMAIL ===");
            System.out.println("À: " + user.getEmail());
            System.out.println("Sujet: " + subject);
            System.out.println("Mode simulation: l'email de confirmation d'inscription serait envoyé si la configuration était activée.");
            System.out.println("Pièces jointes: PDF avec détails et QR code");
            System.out.println("==============================");
            return true;
        }
        
        try {
            // Générer les pièces jointes
            // 1. QR Code
            String qrFilename = "evenement_" + event.getId() + "_qrcode.png";
            byte[] qrCodeData = utils.PDFGenerator.generateQRCode(user, event, null);
            
            // 2. PDF avec détails
            String pdfFilename = "evenement_" + event.getId() + "_details.pdf";
            byte[] pdfData = utils.PDFGenerator.generateEventDetailsPDF(user, event, null);
            
            // Préparer les pièces jointes
            EmailAttachment[] attachments = {
                new EmailAttachment(pdfFilename, "application/pdf", pdfData),
                new EmailAttachment(qrFilename, "image/png", qrCodeData)
            };
            
            // Envoyer l'email avec les pièces jointes
            return sendEmailWithAttachments(user.getEmail(), subject, content, attachments);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la préparation des pièces jointes: " + e.getMessage());
            e.printStackTrace();
            
            // Essayer d'envoyer l'email sans pièces jointes en cas d'erreur
            return sendEmail(user.getEmail(), subject, content);
        }
    }
    
    /**
     * Teste la configuration email pour vérifier si la connexion au serveur SMTP fonctionne
     * Cette méthode n'envoie pas réellement d'email, elle vérifie seulement la connexion
     * 
     * @return true si la connexion au serveur SMTP a réussi
     */
    public boolean testConnection() {
        if (!EmailConfig.ENABLE_EMAIL) {
            System.out.println("Test email: non effectué - Service email désactivé");
            return false;
        }
        
        if (!EmailConfig.isConfigValid()) {
            System.out.println("Test email: non effectué - Configuration incomplète");
            return false;
        }
        
        System.out.println("Test de connexion au serveur SMTP " + EmailConfig.SMTP_HOST + ":" + EmailConfig.SMTP_PORT + "...");
        
        try {
            Session session = getSession();
            if (session == null) {
                System.err.println("Test email échoué: impossible d'obtenir une session");
                return false;
            }
            
            // Obtenir un transport et se connecter seulement (sans envoyer d'email)
            Transport transport = session.getTransport("smtp");
            
            System.out.println("Tentative de connexion avec " + EmailConfig.EMAIL_USERNAME + "...");
            transport.connect(
                EmailConfig.SMTP_HOST,
                Integer.parseInt(EmailConfig.SMTP_PORT),
                EmailConfig.EMAIL_USERNAME,
                EmailConfig.EMAIL_PASSWORD
            );
            
            boolean isConnected = transport.isConnected();
            
            if (isConnected) {
                System.out.println("✓ Test email réussi: connexion établie au serveur SMTP");
                transport.close();
                return true;
            } else {
                System.err.println("✖ Test email échoué: échec de connexion");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("✖ Test email échoué: " + e.getMessage());
            
            if (EmailConfig.DEBUG_ENABLED) {
                e.printStackTrace();
                
                // Afficher des suggestions spécifiques basées sur l'erreur
                if (e.getMessage().contains("IOException") || e.getMessage().contains("Connection timed out")) {
                    System.err.println("\nSuggestion: Vérifiez votre connexion Internet et vos paramètres de pare-feu.");
                } else if (e.getMessage().contains("AuthenticationFailed") || e.getMessage().contains("535")) {
                    System.err.println("\nSuggestion: Vérifiez votre nom d'utilisateur et mot de passe. Pour Gmail, assurez-vous d'utiliser un mot de passe d'application.");
                } else if (e.getMessage().contains("Could not convert socket")) {
                    System.err.println("\nSuggestion: Problème de SSL/TLS. Essayez de changer de port (465 pour SSL ou 587 pour TLS).");
                }
            }
            
            return false;
        }
    }
    
    /**
     * Effectue un diagnostic complet de la configuration email
     * @return Un rapport de diagnostic
     */
    public String runDiagnostic() {
        StringBuilder report = new StringBuilder();
        report.append("=== DIAGNOSTIC EMAIL ===\n");
        
        // Vérifier la configuration de base
        report.append("Configuration activée: ").append(EmailConfig.ENABLE_EMAIL).append("\n");
        report.append("Mode débogage: ").append(EmailConfig.DEBUG_ENABLED).append("\n");
        report.append("Serveur SMTP: ").append(EmailConfig.SMTP_HOST).append(":").append(EmailConfig.SMTP_PORT).append("\n");
        report.append("Utilisateur: ").append(EmailConfig.EMAIL_USERNAME).append("\n");
        report.append("Mot de passe: ").append(EmailConfig.EMAIL_PASSWORD.replaceAll(".", "*")).append("\n\n");
        
        // Vérifier la connexion réseau au serveur SMTP
        report.append("Test de connectivité réseau:\n");
        try {
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(EmailConfig.SMTP_HOST, Integer.parseInt(EmailConfig.SMTP_PORT)), 5000);
            report.append("✓ Connexion réseau établie\n");
            socket.close();
        } catch (Exception e) {
            report.append("✖ Échec de connexion réseau: ").append(e.getMessage()).append("\n");
            report.append("  Suggestion: Vérifiez votre connexion Internet et vos paramètres de pare-feu\n\n");
            return report.toString(); // On s'arrête ici si la connexion réseau échoue
        }
        
        // Obtenir les propriétés SMTP
        report.append("\nPropriétés SMTP configurées:\n");
        report.append("- SSL enabled: ").append(properties.getProperty("mail.smtp.ssl.enable", "non défini")).append("\n");
        report.append("- SSL trust: ").append(properties.getProperty("mail.smtp.ssl.trust", "non défini")).append("\n");
        report.append("- Socket factory: ").append(properties.getProperty("mail.smtp.socketFactory.class", "non défini")).append("\n");
        report.append("- Socket factory fallback: ").append(properties.getProperty("mail.smtp.socketFactory.fallback", "non défini")).append("\n");
        report.append("- STARTTLS enabled: ").append(properties.getProperty("mail.smtp.starttls.enable", "non défini")).append("\n");
        report.append("- SSL protocols: ").append(properties.getProperty("mail.smtp.ssl.protocols", "non défini")).append("\n\n");
        
        // Test de création de session
        try {
            Session session = getSession();
            if (session != null) {
                report.append("✓ Session email créée avec succès\n");
            } else {
                report.append("✖ Échec de création de session email\n");
                return report.toString();
            }
        } catch (Exception e) {
            report.append("✖ Exception lors de la création de session: ").append(e.getMessage()).append("\n");
            return report.toString();
        }
        
        // Test d'authentification
        try {
            report.append("\nTest d'authentification SMTP:\n");
            Session session = getSession();
            Transport transport = session.getTransport("smtp");
            
            // On teste uniquement l'authentification sans envoyer d'email
            report.append("Tentative d'authentification...\n");
            transport.connect(
                EmailConfig.SMTP_HOST,
                Integer.parseInt(EmailConfig.SMTP_PORT),
                EmailConfig.EMAIL_USERNAME,
                EmailConfig.EMAIL_PASSWORD
            );
            
            boolean isConnected = transport.isConnected();
            if (isConnected) {
                report.append("✓ Authentification réussie\n");
                transport.close();
            } else {
                report.append("✖ Authentification échouée (transport non connecté)\n");
            }
        } catch (Exception e) {
            report.append("✖ Exception lors de l'authentification: ").append(e.getMessage()).append("\n");
            
            // Suggestions basées sur l'erreur
            if (e.getMessage().contains("535")) {
                report.append("  Suggestion: Pour Gmail, vérifiez que vous utilisez un mot de passe d'application.\n");
                report.append("  Les mots de passe d'application peuvent être générés dans votre compte Google:\n");
                report.append("  https://myaccount.google.com/apppasswords\n");
            } else if (e.getMessage().contains("javax.net.ssl")) {
                report.append("  Suggestion: Problème de configuration SSL.\n");
                report.append("  Essayez de changer le port à 587 et d'utiliser STARTTLS à la place.\n");
            }
        }
        
        report.append("\n=== FIN DU DIAGNOSTIC ===\n");
        return report.toString();
    }
    
    /**
     * Exécute un test de diagnostic et affiche le résultat dans la console
     */
    public void runDiagnosticAndPrint() {
        System.out.println("\n");
        System.out.println(runDiagnostic());
        System.out.println("\n");
    }
    
    /**
     * Tente d'envoyer un email de test pour vérifier la configuration
     * @param to L'adresse email de test (destinataire)
     * @return true si le test a réussi
     */
    public boolean sendTestEmail(String to) {
        if (to == null || to.isEmpty() || !to.contains("@")) {
            System.err.println("Adresse email de test invalide");
            return false;
        }
        
        // Exécuter d'abord le diagnostic
        runDiagnosticAndPrint();
        
        String subject = "Test de configuration email";
        String content = 
                "<html><body>" +
                "<h2>Test de configuration email</h2>" +
                "<p>Cet email confirme que la configuration de votre service d'email fonctionne correctement.</p>" +
                "<p>Date et heure du test: " + java.time.LocalDateTime.now() + "</p>" +
                "</body></html>";
        
        System.out.println("Envoi d'un email de test à " + to + "...");
        boolean result = sendEmail(to, subject, content);
        
        if (result) {
            System.out.println("✅ Email de test envoyé avec succès");
        } else {
            System.err.println("❌ Échec de l'envoi de l'email de test");
        }
        
        return result;
    }
    
    /**
     * Génère une URL pour accéder aux détails de l'événement et au QR code en ligne
     * @param eventId ID de l'événement
     * @param userId ID de l'utilisateur
     * @return URL formatée
     */
    private String generateEventDetailsUrl(int eventId, int userId) {
        try {
            // Dans un environnement de production, ceci serait une URL réelle vers votre application
            // Pour l'exemple, nous utilisons une URL fictive qui pourrait être implémentée plus tard
            String baseUrl = "http://evenements.example.com/ticket";
            String token = java.util.Base64.getEncoder().encodeToString(
                    (eventId + ":" + userId + ":" + System.currentTimeMillis()).getBytes());
            
            return baseUrl + "?event=" + eventId + "&user=" + userId + "&token=" + token;
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération de l'URL: " + e.getMessage());
            return "#";
        }
    }
} 