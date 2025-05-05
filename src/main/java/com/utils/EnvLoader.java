package com.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class EnvLoader {
    public static void load() {
        try (Stream<String> stream = Files.lines(Paths.get("src/main/resources/.env"))) {
            stream.filter(line -> line.contains("="))
                    .forEach(line -> {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            System.setProperty(parts[0], parts[1]);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du fichier .env: " + e.getMessage());
        }
    }
}