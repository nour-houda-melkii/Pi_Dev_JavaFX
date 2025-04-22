package controllers;

import Services.DataPersistenceService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataHistoryController {
    @FXML
    private ListView<HistoryEntryItem> historyListView;

    @FXML
    private TextArea detailsTextArea;

    @FXML
    private Label titleLabel;

    @FXML
    private Button closeButton;

    @FXML
    private Button exportPdfButton;

    @FXML
    private ComboBox<String> filterComboBox;

    private DataPersistenceService dataPersistenceService;
    private List<String> allHistoryEntries;

    @FXML
    public void initialize() {
        dataPersistenceService = new DataPersistenceService();
        loadHistoryData();

        // Set up the filter combo box
        ObservableList<String> filterOptions = FXCollections.observableArrayList(
                "All Entries", "Product Added", "Product Updated", "Product Deleted",
                "Category Added", "Category Updated", "Category Deleted"
        );
        filterComboBox.setItems(filterOptions);
        filterComboBox.setValue("All Entries");
        filterComboBox.setOnAction(event -> filterHistoryEntries());

        // Set up selection event for the list view
        historyListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        detailsTextArea.setText(formatEntryDetails(newValue.getFullEntry()));
                    }
                }
        );

        // Set up export PDF button
        exportPdfButton.setOnAction(event -> exportToPdf());

        // Set up close button
        closeButton.setOnAction(event -> {
            ((Stage) closeButton.getScene().getWindow()).close();
        });
    }

    private void loadHistoryData() {
        allHistoryEntries = dataPersistenceService.getAllHistoryEntries();
        updateListView(allHistoryEntries);
    }

    private void updateListView(List<String> entries) {
        ObservableList<HistoryEntryItem> observableEntries = FXCollections.observableArrayList();

        for (String entry : entries) {
            HistoryEntryItem item = parseHistoryEntry(entry);
            if (item != null) {
                observableEntries.add(item);
            }
        }

        historyListView.setItems(observableEntries);
        titleLabel.setText("Data History - " + entries.size() + " entries");
    }

    private HistoryEntryItem parseHistoryEntry(String entry) {
        // Pattern to extract timestamp and action
        Pattern pattern = Pattern.compile("\\[(.*?)\\] (.*?): .*");
        Matcher matcher = pattern.matcher(entry);

        if (matcher.find()) {
            String timestamp = matcher.group(1);
            String action = matcher.group(2);
            String summary = getSummaryFromEntry(entry);

            return new HistoryEntryItem(timestamp, action, summary, entry);
        }

        return null;
    }

    private String getSummaryFromEntry(String entry) {
        // Extract a meaningful summary from the entry
        if (entry.contains("PRODUCT ADDED")) {
            Pattern pattern = Pattern.compile("Name=(.*?),");
            Matcher matcher = pattern.matcher(entry);
            if (matcher.find()) {
                return "Added product: " + matcher.group(1);
            }
        } else if (entry.contains("PRODUCT UPDATED")) {
            Pattern pattern = Pattern.compile("NEW: Name=(.*?),");
            Matcher matcher = pattern.matcher(entry);
            if (matcher.find()) {
                return "Updated product: " + matcher.group(1);
            }
        } else if (entry.contains("PRODUCT DELETED")) {
            Pattern pattern = Pattern.compile("Name=(.*?),");
            Matcher matcher = pattern.matcher(entry);
            if (matcher.find()) {
                return "Deleted product: " + matcher.group(1);
            }
        } else if (entry.contains("CATEGORY ADDED")) {
            Pattern pattern = Pattern.compile("Name=(.*?),");
            Matcher matcher = pattern.matcher(entry);
            if (matcher.find()) {
                return "Added category: " + matcher.group(1);
            }
        } else if (entry.contains("CATEGORY UPDATED")) {
            Pattern pattern = Pattern.compile("NEW: Name=(.*?),");
            Matcher matcher = pattern.matcher(entry);
            if (matcher.find()) {
                return "Updated category: " + matcher.group(1);
            }
        } else if (entry.contains("CATEGORY DELETED")) {
            Pattern pattern = Pattern.compile("Name=(.*?),");
            Matcher matcher = pattern.matcher(entry);
            if (matcher.find()) {
                return "Deleted category: " + matcher.group(1);
            }
        }

        return "Unknown operation";
    }

    private String formatEntryDetails(String entry) {
        // Format the entry details with better readability
        String formattedEntry = entry;

        // Replace tabs with proper indentation
        formattedEntry = formattedEntry.replace("\t", "    ");

        // Add spacing between sections
        if (entry.contains("UPDATED")) {
            formattedEntry = formattedEntry.replace("OLD:", "\nOLD:");
            formattedEntry = formattedEntry.replace("NEW:", "\nNEW:");
        }

        // Format property lists for better readability
        formattedEntry = formattedEntry.replace(", ", ",\n    ");

        return formattedEntry;
    }

    private void filterHistoryEntries() {
        String filter = filterComboBox.getValue();

        if (filter.equals("All Entries")) {
            updateListView(allHistoryEntries);
            return;
        }

        List<String> filteredEntries = new ArrayList<>();
        String filterKeyword = filter.toUpperCase().replace(" ", "_");

        for (String entry : allHistoryEntries) {
            if (entry.contains(filterKeyword)) {
                filteredEntries.add(entry);
            }
        }

        updateListView(filteredEntries);
    }

    private void exportToPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        // Set default filename with current date and time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String defaultFileName = "data_history_" + LocalDateTime.now().format(formatter) + ".pdf";
        fileChooser.setInitialFileName(defaultFileName);

        // Show save dialog
        File file = fileChooser.showSaveDialog(exportPdfButton.getScene().getWindow());

        if (file != null) {
            try {
                createPdf(file.getAbsolutePath());

                // Show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Data history has been exported to PDF successfully!");
                alert.showAndWait();
            } catch (Exception e) {
                // Show error alert
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText(null);
                alert.setContentText("Failed to export data history: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void createPdf(String filePath) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));

        document.open();

        // Add title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("Data History Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        // Add generation timestamp
        Font timestampFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
        Paragraph timestamp = new Paragraph("Generated on: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                timestampFont);
        timestamp.setAlignment(Element.ALIGN_CENTER);
        document.add(timestamp);

        document.add(Chunk.NEWLINE);

        // Add summary table
        addSummaryTable(document);

        document.add(Chunk.NEWLINE);

        // Add detailed entries
        addDetailedEntries(document);

        document.close();
    }

    private void addSummaryTable(Document document) throws DocumentException {
        // Create summary of operations
        Map<String, Integer> operationCounts = new HashMap<>();

        for (String entry : allHistoryEntries) {
            String operationType = "Unknown";

            if (entry.contains("PRODUCT ADDED")) operationType = "Product Added";
            else if (entry.contains("PRODUCT UPDATED")) operationType = "Product Updated";
            else if (entry.contains("PRODUCT DELETED")) operationType = "Product Deleted";
            else if (entry.contains("CATEGORY ADDED")) operationType = "Category Added";
            else if (entry.contains("CATEGORY UPDATED")) operationType = "Category Updated";
            else if (entry.contains("CATEGORY DELETED")) operationType = "Category Deleted";

            operationCounts.put(operationType, operationCounts.getOrDefault(operationType, 0) + 1);
        }

        // Create the summary table
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Add header
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase("Operation Type", headerFont));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("Count", headerFont));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);

        // Add data rows
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 10);
        for (Map.Entry<String, Integer> entry : operationCounts.entrySet()) {
            cell = new PdfPCell(new Phrase(entry.getKey(), cellFont));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setPadding(5);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase(entry.getValue().toString(), cellFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // Add total row
        int total = operationCounts.values().stream().mapToInt(Integer::intValue).sum();

        cell = new PdfPCell(new Phrase("Total", headerFont));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(5);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase(String.valueOf(total), headerFont));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);

        // Add the table to the document
        Paragraph summaryTitle = new Paragraph("Summary of Operations", headerFont);
        summaryTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(summaryTitle);
        document.add(Chunk.NEWLINE);
        document.add(table);
    }

    private void addDetailedEntries(Document document) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font entryFont = new Font(Font.FontFamily.HELVETICA, 10);
        Font timestampFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC);

        Paragraph detailsTitle = new Paragraph("Detailed History Entries", sectionFont);
        detailsTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(detailsTitle);
        document.add(Chunk.NEWLINE);

        for (String entry : allHistoryEntries) {
            HistoryEntryItem item = parseHistoryEntry(entry);
            if (item == null) continue;

            // Create a section for each entry
            PdfPTable entryTable = new PdfPTable(1);
            entryTable.setWidthPercentage(100);

            // Add timestamp and type
            Paragraph headerPara = new Paragraph();
            headerPara.add(new Chunk(item.getAction() + " - ", sectionFont));
            headerPara.add(new Chunk(item.getTimestamp(), timestampFont));

            PdfPCell cell = new PdfPCell(headerPara);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            entryTable.addCell(cell);

            // Add details
            String formattedDetails = formatEntryDetails(item.getFullEntry());
            cell = new PdfPCell(new Paragraph(formattedDetails, entryFont));
            cell.setPadding(5);
            entryTable.addCell(cell);

            document.add(entryTable);
            document.add(Chunk.NEWLINE);
        }
    }


    public static class HistoryEntryItem {
        private final String timestamp;
        private final String action;
        private final String summary;
        private final String fullEntry;

        public HistoryEntryItem(String timestamp, String action, String summary, String fullEntry) {
            this.timestamp = timestamp;
            this.action = action;
            this.summary = summary;
            this.fullEntry = fullEntry;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getAction() {
            return action;
        }

        public String getSummary() {
            return summary;
        }

        public String getFullEntry() {
            return fullEntry;
        }

        @Override
        public String toString() {
            return timestamp + " - " + summary;
        }
    }
}