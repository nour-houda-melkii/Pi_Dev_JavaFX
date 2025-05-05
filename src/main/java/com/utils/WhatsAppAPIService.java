package com.utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WhatsAppAPIService {

    private static final String API_URL = "https://wapi.dev/api/send";
    private static final String API_TOKEN = "ton_api_token"; // ⚡ Mets ici ton token donné par wapi.dev

    public static void sendWhatsAppMessage(String phoneNumber, String message) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_TOKEN);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonPayload = "{"
                    + "\"phone\": \"" + phoneNumber + "\","
                    + "\"message\": \"" + message + "\""
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("✅ Message WhatsApp envoyé avec succès");
            } else {
                System.err.println("❌ Erreur lors de l'envoi WhatsApp. Code : " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("❌ Exception lors de l'envoi WhatsApp : " + e.getMessage());
        }
    }
}

