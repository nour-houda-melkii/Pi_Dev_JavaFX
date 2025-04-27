package utils;

/**
 * Configuration pour le service d'email
 */
public class EmailConfig {
    // Informations de connexion SMTP
    public static final String EMAIL_USERNAME = "";
    // Le mot de passe sans espaces pour éviter les problèmes d'échappement
    public static final String EMAIL_PASSWORD = "";
    
    // Configuration du serveur SMTP (Gmail par défaut)
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final String SMTP_PORT = "587";
    
    // Adresse de l'expéditeur (From)
    public static final String FROM_EMAIL = EMAIL_USERNAME;
    public static final String FROM_NAME = "Gestionnaire d'Événements";
    
    // Paramètres d'email
    public static final boolean ENABLE_EMAIL = true; // Activé pour permettre l'envoi d'emails
    
    // Propriétés supplémentaires pour le débogage et la configuration SMTP
    public static final boolean DEBUG_ENABLED = true;
    public static final boolean SSL_ENABLED = true;
    
    /**
     * Vérifie si la configuration est valide pour envoyer des emails
     * @return true si la configuration permet l'envoi d'emails
     */
    public static boolean isConfigValid() {
        if (!ENABLE_EMAIL) {
            return false;
        }
        
        // Vérification des identifiants
        boolean hasValidCredentials = !EMAIL_USERNAME.isEmpty() && !EMAIL_PASSWORD.isEmpty();
        
        // Vérification du serveur SMTP
        boolean hasValidServer = !SMTP_HOST.isEmpty() && !SMTP_PORT.isEmpty();
        
        // Vérification du port (doit être un nombre)
        boolean hasValidPort = false;
        try {
            int port = Integer.parseInt(SMTP_PORT);
            hasValidPort = port > 0 && port < 65536; // Ports valides: 1-65535
        } catch (NumberFormatException e) {
            hasValidPort = false;
        }
        
        // Vérification de l'adresse expéditeur
        boolean hasValidSender = !FROM_EMAIL.isEmpty() && FROM_EMAIL.contains("@");
        
        if (DEBUG_ENABLED && !hasValidCredentials) {
            System.err.println("Configuration email invalide: identifiants manquants");
        }
        
        if (DEBUG_ENABLED && (!hasValidServer || !hasValidPort)) {
            System.err.println("Configuration email invalide: serveur SMTP ou port incorrect");
        }
        
        if (DEBUG_ENABLED && !hasValidSender) {
            System.err.println("Configuration email invalide: adresse expéditeur incorrecte");
        }
        
        return hasValidCredentials && hasValidServer && hasValidPort && hasValidSender;
    }
} 