package utils;

import models.Event;
import models.User;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

// Imports for QR code generation with ZXing
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

// Imports for PDF generation with iText
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.BaseColor;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe utilitaire pour générer des PDF avec des détails d'événement et QR code
 */
public class PDFGenerator {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
    
    /**
     * Génère un PDF avec les détails de l'événement et un QR code
     * @param user L'utilisateur inscrit
     * @param event L'événement auquel l'utilisateur est inscrit
     * @param outputPath Le chemin où sauvegarder le PDF (ou null pour retourner le PDF comme byte[])
     * @return Les données du PDF si outputPath est null, sinon null
     */
    public static byte[] generateEventDetailsPDF(User user, Event event, String outputPath) {
        try {
            Document document = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            if (outputPath != null) {
                PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            } else {
                PdfWriter.getInstance(document, baos);
            }
            
            document.open();
            
            // Styles de texte
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLUE);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            
            // Titre
            Paragraph title = new Paragraph("Confirmation d'inscription", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Informations du participant
            Paragraph participant = new Paragraph("Participant: " + user.getNom() + " " + user.getPrenom(), headerFont);
            participant.setSpacingAfter(15);
            document.add(participant);
            
            // Détails de l'événement
            PdfPTable eventTable = new PdfPTable(2);
            eventTable.setWidthPercentage(100);
            eventTable.setSpacingBefore(10);
            eventTable.setSpacingAfter(10);
            
            // Style pour l'en-tête de table
            PdfPCell cell = new PdfPCell(new Phrase("Détails de l'événement", headerFont));
            cell.setBackgroundColor(new BaseColor(230, 230, 250));
            cell.setPadding(8);
            cell.setColspan(2);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            eventTable.addCell(cell);
            
            // Ajout des détails de l'événement
            addEventDetail(eventTable, "Titre", event.getTitle(), normalFont);
            addEventDetail(eventTable, "Date de début", event.getStartDate().format(dateFormatter), normalFont);
            addEventDetail(eventTable, "Date de fin", event.getEndDate().format(dateFormatter), normalFont);
            addEventDetail(eventTable, "Lieu", event.getLocation(), normalFont);
            addEventDetail(eventTable, "Description", event.getDescription(), normalFont);
            
            document.add(eventTable);
            
            // Génération et ajout du QR code
            byte[] qrCodeData = generateQRCode(user, event, null);
            if (qrCodeData != null) {
                Image qrCodeImage = Image.getInstance(qrCodeData);
                qrCodeImage.setAlignment(Element.ALIGN_CENTER);
                qrCodeImage.scaleToFit(200, 200);
                
                Paragraph qrTitle = new Paragraph("Présentez ce QR code à l'entrée de l'événement:", normalFont);
                qrTitle.setAlignment(Element.ALIGN_CENTER);
                qrTitle.setSpacingBefore(20);
                qrTitle.setSpacingAfter(10);
                document.add(qrTitle);
                document.add(qrCodeImage);
            }
            
            // Note de bas de page
            Paragraph footer = new Paragraph("Merci de votre participation!", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);
            
            document.close();
            
            if (outputPath != null) {
                return null;
            } else {
                return baos.toByteArray();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Ajoute une ligne de détail à la table d'événement
     */
    private static void addEventDetail(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label + ":", font));
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(new BaseColor(240, 240, 240));
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setPadding(5);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    /**
     * Génère un QR code basé sur les informations de l'utilisateur et de l'événement
     * @param user L'utilisateur
     * @param event L'événement
     * @param outputPath Chemin de sortie pour l'image du QR code (ou null)
     * @return Les données de l'image si outputPath est null, sinon null
     */
    public static byte[] generateQRCode(User user, Event event, String outputPath) {
        try {
            // Données à encoder dans le QR code
            String qrData = String.format("EVENT:%d;USER:%d;NAME:%s %s;DATE:%s",
                event.getId(), user.getId(), user.getNom(), user.getPrenom(),
                event.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            // Configuration du QR code
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            
            // Création du QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 200, 200, hints);
            
            // Conversion en image
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            
            // Sauvegarder si un chemin est fourni
            if (outputPath != null) {
                File outputFile = new File(outputPath);
                ImageIO.write(qrImage, "png", outputFile);
                return null;
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(qrImage, "png", baos);
                return baos.toByteArray();
            }
            
        } catch (WriterException | IOException e) {
            System.err.println("Erreur lors de la génération du QR code: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
} 