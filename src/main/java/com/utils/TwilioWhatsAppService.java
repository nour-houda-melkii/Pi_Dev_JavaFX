package com.utils;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.cdimascio.dotenv.Dotenv;

public class TwilioWhatsAppService {

    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final String ACCOUNT_SID = dotenv.get("TWILIO_ACCOUNT_SID");
    private static final String AUTH_TOKEN = dotenv.get("TWILIO_AUTH_TOKEN");
    private static final String FROM_NUMBER = "whatsapp:" + dotenv.get("TWILIO_WHATSAPP_NUMBER");

    static {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    public static void sendWhatsAppMessage(String toNumber, String message) {
        try {
            Message.creator(
                    new PhoneNumber("whatsapp:" + toNumber),
                    new PhoneNumber(FROM_NUMBER),
                    message
            ).create();
            System.out.println("Message WhatsApp envoyé à " + toNumber);
        } catch (Exception e) {
            System.err.println("Échec d'envoi WhatsApp : " + e.getMessage());
        }
    }
}