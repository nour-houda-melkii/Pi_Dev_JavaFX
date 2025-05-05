package com.utils;

import com.models.User;

public class WelcomeEmailService {
    private static final String FROM_EMAIL = "no-reply@votreapp.com";
    private static final String WELCOME_SUBJECT = "Bienvenue sur notre plateforme";

    public static void sendWelcomeEmail(User user, String plainPassword) {
        try {
            String content = buildEmailContent(user, plainPassword);
            EmailService.sendEmail(FROM_EMAIL, user.getEmail(), WELCOME_SUBJECT, content);
            System.out.println("Email de bienvenue envoyé à " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de bienvenue: " + e.getMessage());
            throw new RuntimeException("Échec de l'envoi de l'email de bienvenue", e);
        }
    }

    private static String buildEmailContent(User user, String plainPassword) {
        return "<html>"
                + "<body>"
                + "<h2>Bonjour " + user.getFirstName() + " " + user.getLastName() + ",</h2>"
                + "<p>Votre compte a été créé avec succès.</p>"
                + "<p><strong>Identifiants de connexion:</strong></p>"
                + "<ul>"
                + "<li>Email: " + user.getEmail() + "</li>"
                + "<li>Mot de passe temporaire: <code>" + plainPassword + "</code></li>"
                + "</ul>"
                + "<p style='color: red;'>Pour des raisons de sécurité, veuillez changer votre mot de passe après votre première connexion.</p>"
                + "<p>Cordialement,<br>L'équipe de support</p>"
                + "</body>"
                + "</html>";
    }
}