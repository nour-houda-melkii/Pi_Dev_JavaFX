package utils;

import entity.Reclamation;
import entity.TypeReclamation;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.image.WritableImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Classe utilitaire pour exporter les statistiques en PDF
 */
public class StatisticsExporter {

    /**
     * Exporte les statistiques sous forme de PDF
     * @param filePath Le chemin où sauvegarder le fichier PDF
     * @param reclamations Liste des réclamations
     * @param typeReclamations Liste des types de réclamations
     * @param pieChart Graphique circulaire à exporter
     * @param typeBarChart Graphique à barres par type à exporter
     * @param monthlyBarChart Graphique à barres mensuel à exporter
     * @throws IOException En cas d'erreur lors de la création du PDF
     */
    public static void exportToPdf(String filePath,
                                   List<Reclamation> reclamations,
                                   List<TypeReclamation> typeReclamations,
                                   PieChart pieChart,
                                   BarChart<String, Number> typeBarChart,
                                   BarChart<String, Number> monthlyBarChart) throws IOException {

        try (PDDocument document = new PDDocument()) {
            // Page de garde
            PDPage coverPage = new PDPage(PDRectangle.A4);
            document.addPage(coverPage);

            PDPageContentStream coverContent = new PDPageContentStream(document, coverPage);

            // Titre du rapport
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA_BOLD, 24);
            coverContent.newLineAtOffset(50, 750);
            coverContent.showText("Rapport Statistique des Réclamations");
            coverContent.endText();

            // Date de génération
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA, 14);
            coverContent.newLineAtOffset(50, 720);
            coverContent.showText("Généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            coverContent.endText();

            // Résumé des statistiques
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA_BOLD, 16);
            coverContent.newLineAtOffset(50, 650);
            coverContent.showText("Résumé des Statistiques");
            coverContent.endText();

            // Nombre total de réclamations
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA, 12);
            coverContent.newLineAtOffset(70, 630);
            coverContent.showText("Nombre total de réclamations: " + reclamations.size());
            coverContent.endText();

            // Répartition par type de réclamation
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA_BOLD, 14);
            coverContent.newLineAtOffset(50, 590);
            coverContent.showText("Répartition par Type de Réclamation");
            coverContent.endText();

            // Calcul des statistiques par type
            Map<String, Integer> typeCounts = new HashMap<>();
            for (TypeReclamation type : typeReclamations) {
                typeCounts.put(type.getNom(), 0);
            }

            for (Reclamation reclamation : reclamations) {
                String typeName = reclamation.getTypeReclamationName();
                typeCounts.put(typeName, typeCounts.getOrDefault(typeName, 0) + 1);
            }

            // Afficher les statistiques par type
            float yPosition = 570;
            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                coverContent.beginText();
                coverContent.setFont(PDType1Font.HELVETICA, 12);
                coverContent.newLineAtOffset(70, yPosition);

                // Calculer le pourcentage
                float percentage = reclamations.isEmpty() ? 0 : (entry.getValue() * 100f / reclamations.size());

                coverContent.showText(entry.getKey() + ": " + entry.getValue() +
                        " (" + String.format("%.1f", percentage) + "%)");
                coverContent.endText();

                yPosition -= 20;

                // Vérifier s'il faut passer à une nouvelle page
                if (yPosition < 100) {
                    coverContent.close();

                    PDPage newPage = new PDPage(PDRectangle.A4);
                    document.addPage(newPage);
                    coverContent = new PDPageContentStream(document, newPage);

                    coverContent.beginText();
                    coverContent.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    coverContent.newLineAtOffset(50, 750);
                    coverContent.showText("Répartition par Type de Réclamation (suite)");
                    coverContent.endText();

                    yPosition = 730;
                }
            }

            coverContent.close();

            // Page pour le graphique circulaire
            if (pieChart != null) {
                PDPage chartPage = new PDPage(PDRectangle.A4);
                document.addPage(chartPage);

                PDPageContentStream chartContent = new PDPageContentStream(document, chartPage);

                // Titre de la page
                chartContent.beginText();
                chartContent.setFont(PDType1Font.HELVETICA_BOLD, 16);
                chartContent.newLineAtOffset(50, 750);
                chartContent.showText("Graphique de Répartition par Type");
                chartContent.endText();

                // Capturer le graphique comme image
                WritableImage snapshot = pieChart.snapshot(new SnapshotParameters(), null);
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

                // Ajouter l'image au PDF
                PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);
                chartContent.drawImage(pdImage, 50, 400, 500, 300);

                chartContent.close();
            }

            // Page pour le graphique à barres par type
            if (typeBarChart != null) {
                PDPage barChartPage = new PDPage(PDRectangle.A4);
                document.addPage(barChartPage);

                PDPageContentStream barChartContent = new PDPageContentStream(document, barChartPage);

                // Titre de la page
                barChartContent.beginText();
                barChartContent.setFont(PDType1Font.HELVETICA_BOLD, 16);
                barChartContent.newLineAtOffset(50, 750);
                barChartContent.showText("Graphique à Barres par Type de Réclamation");
                barChartContent.endText();

                // Capturer le graphique comme image
                WritableImage snapshot = typeBarChart.snapshot(new SnapshotParameters(), null);
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

                // Ajouter l'image au PDF
                PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);
                barChartContent.drawImage(pdImage, 50, 400, 500, 300);

                barChartContent.close();
            }

            // Page pour le graphique mensuel
            if (monthlyBarChart != null) {
                PDPage monthlyChartPage = new PDPage(PDRectangle.A4);
                document.addPage(monthlyChartPage);

                PDPageContentStream monthlyChartContent = new PDPageContentStream(document, monthlyChartPage);

                // Titre de la page
                monthlyChartContent.beginText();
                monthlyChartContent.setFont(PDType1Font.HELVETICA_BOLD, 16);
                monthlyChartContent.newLineAtOffset(50, 750);
                monthlyChartContent.showText("Évolution Mensuelle des Réclamations");
                monthlyChartContent.endText();

                // Capturer le graphique comme image
                WritableImage snapshot = monthlyBarChart.snapshot(new SnapshotParameters(), null);
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

                // Ajouter l'image au PDF
                PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);
                monthlyChartContent.drawImage(pdImage, 50, 400, 500, 300);

                // Ajouter une conclusion
                monthlyChartContent.beginText();
                monthlyChartContent.setFont(PDType1Font.HELVETICA_BOLD, 14);
                monthlyChartContent.newLineAtOffset(50, 350);
                monthlyChartContent.showText("Conclusion");
                monthlyChartContent.endText();

                monthlyChartContent.beginText();
                monthlyChartContent.setFont(PDType1Font.HELVETICA, 12);
                monthlyChartContent.newLineAtOffset(50, 330);
                monthlyChartContent.showText("Ce rapport présente une analyse détaillée des réclamations");
                monthlyChartContent.endText();

                monthlyChartContent.beginText();
                monthlyChartContent.setFont(PDType1Font.HELVETICA, 12);
                monthlyChartContent.newLineAtOffset(50, 315);
                monthlyChartContent.showText("enregistrées dans le système. Il permet d'identifier les tendances");
                monthlyChartContent.endText();

                monthlyChartContent.beginText();
                monthlyChartContent.setFont(PDType1Font.HELVETICA, 12);
                monthlyChartContent.newLineAtOffset(50, 300);
                monthlyChartContent.showText("et les problèmes récurrents pour améliorer la qualité du service.");
                monthlyChartContent.endText();

                // Pied de page avec pagination
                monthlyChartContent.beginText();
                monthlyChartContent.setFont(PDType1Font.HELVETICA, 10);
                monthlyChartContent.newLineAtOffset(250, 30);
                monthlyChartContent.showText("Page " + document.getNumberOfPages() + " / " + document.getNumberOfPages());
                monthlyChartContent.endText();

                monthlyChartContent.close();
            }

            // Enregistrer le document
            document.save(filePath);
        }
    }

    /**
     * Exporte les statistiques sous forme de PDF (version texte uniquement)
     * @param filePath Le chemin où sauvegarder le fichier PDF
     * @param reclamations Liste des réclamations
     * @param typeReclamations Liste des types de réclamations
     * @throws IOException En cas d'erreur lors de la création du PDF
     */
    public static void exportTextPdf(String filePath,
                                     List<Reclamation> reclamations,
                                     List<TypeReclamation> typeReclamations) throws IOException {

        try (PDDocument document = new PDDocument()) {
            // Page de garde
            PDPage coverPage = new PDPage(PDRectangle.A4);
            document.addPage(coverPage);

            PDPageContentStream coverContent = new PDPageContentStream(document, coverPage);

            // Titre du rapport
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA_BOLD, 24);
            coverContent.newLineAtOffset(50, 750);
            coverContent.showText("Rapport Statistique des Réclamations");
            coverContent.endText();

            // Date de génération
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA, 14);
            coverContent.newLineAtOffset(50, 720);
            coverContent.showText("Généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            coverContent.endText();

            // Résumé des statistiques
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA_BOLD, 16);
            coverContent.newLineAtOffset(50, 650);
            coverContent.showText("Résumé des Statistiques");
            coverContent.endText();

            // Nombre total de réclamations
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA, 12);
            coverContent.newLineAtOffset(70, 630);
            coverContent.showText("Nombre total de réclamations: " + reclamations.size());
            coverContent.endText();

            // Répartition par type de réclamation
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA_BOLD, 14);
            coverContent.newLineAtOffset(50, 590);
            coverContent.showText("Répartition par Type de Réclamation");
            coverContent.endText();

            // Calcul des statistiques par type
            Map<String, Integer> typeCounts = new HashMap<>();
            for (TypeReclamation type : typeReclamations) {
                typeCounts.put(type.getNom(), 0);
            }

            for (Reclamation reclamation : reclamations) {
                String typeName = reclamation.getTypeReclamationName();
                typeCounts.put(typeName, typeCounts.getOrDefault(typeName, 0) + 1);
            }

            // Afficher les statistiques par type
            float yPosition = 570;
            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                coverContent.beginText();
                coverContent.setFont(PDType1Font.HELVETICA, 12);
                coverContent.newLineAtOffset(70, yPosition);

                // Calculer le pourcentage
                float percentage = reclamations.isEmpty() ? 0 : (entry.getValue() * 100f / reclamations.size());

                coverContent.showText(entry.getKey() + ": " + entry.getValue() +
                        " (" + String.format("%.1f", percentage) + "%)");
                coverContent.endText();

                yPosition -= 20;

                // Vérifier s'il faut passer à une nouvelle page
                if (yPosition < 100) {
                    coverContent.close();

                    PDPage newPage = new PDPage(PDRectangle.A4);
                    document.addPage(newPage);
                    coverContent = new PDPageContentStream(document, newPage);

                    coverContent.beginText();
                    coverContent.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    coverContent.newLineAtOffset(50, 750);
                    coverContent.showText("Répartition par Type de Réclamation (suite)");
                    coverContent.endText();

                    yPosition = 730;
                }
            }

            // Ajouter les informations par mois
            if (yPosition < 200) { // Si pas assez d'espace, passer à une nouvelle page
                coverContent.close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                coverContent = new PDPageContentStream(document, newPage);
                yPosition = 750;
            } else {
                yPosition -= 40; // Espace supplémentaire avant la section suivante
            }

            // Titre des statistiques mensuelles
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA_BOLD, 14);
            coverContent.newLineAtOffset(50, yPosition);
            coverContent.showText("Répartition par Mois");
            coverContent.endText();
            yPosition -= 30;

            // Calculer les statistiques par mois
            Map<String, Long> monthCounts = reclamations.stream()
                    .filter(r -> {
                        try {
                            LocalDate.parse(r.getFormattedDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.groupingBy(
                            r -> {
                                LocalDate date = LocalDate.parse(r.getFormattedDate(),
                                        DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                                return date.getMonth().toString() + " " + date.getYear();
                            },
                            Collectors.counting()
                    ));

            // Afficher les statistiques par mois
            for (Map.Entry<String, Long> entry : monthCounts.entrySet()) {
                if (yPosition < 100) {
                    coverContent.close();
                    PDPage newPage = new PDPage(PDRectangle.A4);
                    document.addPage(newPage);
                    coverContent = new PDPageContentStream(document, newPage);
                    yPosition = 750;

                    coverContent.beginText();
                    coverContent.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    coverContent.newLineAtOffset(50, yPosition);
                    coverContent.showText("Répartition par Mois (suite)");
                    coverContent.endText();
                    yPosition -= 30;
                }

                coverContent.beginText();
                coverContent.setFont(PDType1Font.HELVETICA, 12);
                coverContent.newLineAtOffset(70, yPosition);
                float monthPercentage = reclamations.isEmpty() ? 0 : (entry.getValue() * 100f / reclamations.size());
                coverContent.showText(entry.getKey() + ": " + entry.getValue() +
                        " (" + String.format("%.1f", monthPercentage) + "%)");
                coverContent.endText();
                yPosition -= 20;
            }

            // Ajouter une conclusion
            if (yPosition < 150) { // Si pas assez d'espace pour la conclusion
                coverContent.close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                coverContent = new PDPageContentStream(document, newPage);
                yPosition = 750;
            } else {
                yPosition -= 40; // Espace supplémentaire avant la conclusion
            }

            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA_BOLD, 14);
            coverContent.newLineAtOffset(50, yPosition);
            coverContent.showText("Conclusion");
            coverContent.endText();
            yPosition -= 20;

            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA, 12);
            coverContent.newLineAtOffset(50, yPosition);
            coverContent.showText("Ce rapport présente une analyse détaillée des réclamations");
            coverContent.endText();
            yPosition -= 15;

            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA, 12);
            coverContent.newLineAtOffset(50, yPosition);
            coverContent.showText("enregistrées dans le système. Il permet d'identifier les tendances");
            coverContent.endText();
            yPosition -= 15;

            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA, 12);
            coverContent.newLineAtOffset(50, yPosition);
            coverContent.showText("et les problèmes récurrents pour améliorer la qualité du service.");
            coverContent.endText();

            // Pied de page avec pagination
            coverContent.beginText();
            coverContent.setFont(PDType1Font.HELVETICA, 10);
            coverContent.newLineAtOffset(250, 30);
            coverContent.showText("Page " + document.getNumberOfPages() + " / " + document.getNumberOfPages());
            coverContent.endText();

            coverContent.close();

            // Enregistrer le document
            document.save(filePath);
        }
    }
}