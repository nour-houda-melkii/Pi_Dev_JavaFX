package com.utils;

import com.exceptions.SmsException;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.cdimascio.dotenv.Dotenv;

public class SmsService {
    private final String accountSid;
    private final String authToken;
    private final String twilioPhoneNumber;

    public SmsService() {
        // Charger les variables d'environnement
        Dotenv dotenv = Dotenv.configure()
                .directory("src/main/resources")
                .load();

        this.accountSid = dotenv.get("TWILIO_ACCOUNT_SID");
        this.authToken = dotenv.get("TWILIO_AUTH_TOKEN");
        this.twilioPhoneNumber = dotenv.get("TWILIO_PHONE_NUMBER");

        // Vérifier que les variables sont bien chargées
        if (accountSid == null || authToken == null || twilioPhoneNumber == null) {
            throw new RuntimeException("Les informations Twilio ne sont pas configurées dans le fichier .env");
        }

        // Initialiser Twilio
        Twilio.init(accountSid, authToken);
    }

    public void sendVerificationSms(String phoneNumber, String code) throws SmsException {
        try {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new SmsException("Le numéro de téléphone est invalide");
            }

            phoneNumber = phoneNumber.trim();
            String messageBody = "Votre code de vérification est: " + code +
                    "\nValable 15 minutes.";

            Message message = Message.creator(
                            new PhoneNumber(phoneNumber),
                            new PhoneNumber(twilioPhoneNumber),
                            messageBody)
                    .create();

            System.out.println("SMS envoyé à " + phoneNumber +
                    ", SID: " + message.getSid());

        } catch (Exception e) {
            throw new SmsException("Échec de l'envoi du SMS: " + e.getMessage(), e);
        }
    }
}