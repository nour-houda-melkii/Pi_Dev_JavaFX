package com.utils;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EmailService {
    private static final Properties props = loadConfig();

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get("src/main/resources/.env"))) {
            properties.load(input);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du fichier .env: " + e.getMessage());
            throw new RuntimeException("Impossible de charger la configuration email", e);
        }
        return properties;
    }

    public static void sendVerificationEmail(String toEmail, String verificationCode) throws MessagingException {
        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                props.getProperty("mail.smtp.username"),
                                props.getProperty("mail.smtp.password")
                        );
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(props.getProperty("mail.smtp.username")));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Votre code de vérification");

            String htmlContent = "<html><body>"
                    + "<h2>Réinitialisation de votre mot de passe</h2>"
                    + "<p>Votre code de vérification est : <strong>" + verificationCode + "</strong></p>"
                    + "<p>Ce code expirera dans 15 minutes.</p>"
                    + "<p>Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email.</p>"
                    + "</body></html>";

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("Email envoyé avec succès à " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
            throw e;
        }
    }


    public static void sendEmail(String from, String to, String subject, String htmlContent) throws MessagingException {
        Session session = createSession();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setContent(htmlContent, "text/html; charset=utf-8");

        Transport.send(message);
    }

    // Méthodes helper privées
    private static Session createSession() {
        return Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                getSmtpUsername(),
                                getSmtpPassword()
                        );
                    }
                });
    }

    private static String getSmtpUsername() {
        return props.getProperty("mail.smtp.username");
    }

    private static String getSmtpPassword() {
        return props.getProperty("mail.smtp.password");
    }

    private static String getSmtpHost() {
        return props.getProperty("mail.smtp.host");
    }

    private static String getSmtpPort() {
        return props.getProperty("mail.smtp.port");
    }
}