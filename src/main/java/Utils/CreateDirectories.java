package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Classe utilitaire pour créer les répertoires et fichiers nécessaires pour les statistiques
 * Cette classe peut être utilisée une seule fois pour configurer la structure du projet
 */
public class CreateDirectories {

    public static void main(String[] args) {
        // Chemins des répertoires
        String baseDir = "src/main/resources";
        String viewsDir = baseDir + "/views";
        String stylesDir = baseDir + "/styles";
        String utilsJavaDir = "src/main/java/utils";

        // Créer les répertoires
        createDirectory(viewsDir);
        createDirectory(stylesDir);
        createDirectory(utilsJavaDir);

        // Vérifier si les fichiers existent dans les répertoires du code source du projet courant
        File statsViewFile = new File("statistics_view.fxml");
        File stylesCssFile = new File("statistics-styles.css");
        File statsExporterFile = new File("StatisticsExporter.java");

        // Copier les fichiers vers les bons répertoires
        if (statsViewFile.exists()) {
            copyFile(statsViewFile.getAbsolutePath(), viewsDir + "/statistics_view.fxml");
        } else {
            System.out.println("Fichier statistics_view.fxml non trouvé dans le répertoire courant");
        }

        if (stylesCssFile.exists()) {
            copyFile(stylesCssFile.getAbsolutePath(), stylesDir + "/statistics-styles.css");
        } else {
            System.out.println("Fichier statistics-styles.css non trouvé dans le répertoire courant");
            // Créer un fichier CSS minimal
            createCssFile(stylesDir + "/statistics-styles.css");
        }

        if (statsExporterFile.exists()) {
            copyFile(statsExporterFile.getAbsolutePath(), utilsJavaDir + "/StatisticsExporter.java");
        } else {
            System.out.println("Fichier StatisticsExporter.java non trouvé dans le répertoire courant");
        }

        System.out.println("Configuration des répertoires terminée.");
    }

    private static void createDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println("Répertoire créé: " + dirPath);
            } else {
                System.err.println("Impossible de créer le répertoire: " + dirPath);
            }
        } else {
            System.out.println("Le répertoire existe déjà: " + dirPath);
        }
    }

    private static void copyFile(String sourcePath, String destPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path dest = Paths.get(destPath);
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Fichier copié de " + sourcePath + " vers " + destPath);
        } catch (IOException e) {
            System.err.println("Erreur lors de la copie du fichier: " + e.getMessage());
        }
    }

    private static void createCssFile(String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("/* Styles pour les statistiques */\n\n");
            writer.write(".chart {\n    -fx-padding: 10px;\n}\n\n");
            writer.write(".chart-plot-background {\n    -fx-background-color: transparent;\n}\n\n");
            writer.write(".chart-pie {\n    -fx-border-color: transparent;\n}\n\n");
            writer.write(".default-color0.chart-pie { -fx-pie-color: #3498db; }\n");
            writer.write(".default-color1.chart-pie { -fx-pie-color: #2ecc71; }\n");
            writer.write(".default-color2.chart-pie { -fx-pie-color: #e74c3c; }\n");
            writer.write(".default-color3.chart-pie { -fx-pie-color: #f39c12; }\n");
            writer.write(".default-color4.chart-pie { -fx-pie-color: #9b59b6; }\n");

            System.out.println("Fichier CSS minimal créé: " + filePath);
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du fichier CSS: " + e.getMessage());
        }
    }
}