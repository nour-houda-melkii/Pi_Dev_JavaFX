package com.utils;

import java.security.SecureRandom;

public class PasswordGenerator {
    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";


    private static final String PASSWORD_ALLOW_BASE = CHAR_LOWER + CHAR_UPPER + NUMBER;
    private static final SecureRandom random = new SecureRandom();

    public static String generateSecurePassword() {
        StringBuilder sb = new StringBuilder(12);

        // Au moins une minuscule
        sb.append(CHAR_LOWER.charAt(random.nextInt(CHAR_LOWER.length())));

        // Au moins une majuscule
        sb.append(CHAR_UPPER.charAt(random.nextInt(CHAR_UPPER.length())));

        // Au moins un chiffre
        sb.append(NUMBER.charAt(random.nextInt(NUMBER.length())));


        // Remplir le reste
        for (int i = 0; i < 8; i++) {
            sb.append(PASSWORD_ALLOW_BASE.charAt(random.nextInt(PASSWORD_ALLOW_BASE.length())));
        }

        // Mélanger le résultat
        return shuffleString(sb.toString());
    }

    private static String shuffleString(String input) {
        char[] characters = input.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            int randomIndex = random.nextInt(characters.length);
            char temp = characters[i];
            characters[i] = characters[randomIndex];
            characters[randomIndex] = temp;
        }
        return new String(characters);
    }
}