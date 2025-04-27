package controllers;

import Services.DataPersistenceService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

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
    private Button exportExcelButton;

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

        // Set up export Excel button
        exportExcelButton.setOnAction(event -> exportToExcel());

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

    private void exportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

        // Set default filename with current date and time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String defaultFileName = "data_history_" + LocalDateTime.now().format(formatter) + ".xlsx";
        fileChooser.setInitialFileName(defaultFileName);

        // Show save dialog
        File file = fileChooser.showSaveDialog(exportExcelButton.getScene().getWindow());

        if (file != null) {
            try {
                createExcel(file.getAbsolutePath());

                // Show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Data history has been exported to Excel successfully!");
                alert.showAndWait();
            } catch (Exception e) {
                // Show error alert
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText(null);
                alert.setContentText("Failed to export data history: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
            }
        }
    }

    private void createExcel(String filePath) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create summary sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(workbook, summarySheet);

            // Create detailed entries sheet
            Sheet detailsSheet = workbook.createSheet("Detailed Entries");
            createDetailsSheet(workbook, detailsSheet);

            // Auto-size columns
            for (int i = 0; i < summarySheet.getRow(0).getPhysicalNumberOfCells(); i++) {
                summarySheet.autoSizeColumn(i);
            }
            for (int i = 0; i < detailsSheet.getRow(0).getPhysicalNumberOfCells(); i++) {
                detailsSheet.autoSizeColumn(i);
            }

            // Write the output to a file
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
    }

    private void createSummarySheet(Workbook workbook, Sheet sheet) {
        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Create title row
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Data History Report");

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        // Merge cells for title
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // Add generation timestamp
        Row timestampRow = sheet.createRow(1);
        timestampRow.createCell(0).setCellValue("Generated on:");
        timestampRow.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Add empty row
        sheet.createRow(2);

        // Create summary table header
        Row headerRow = sheet.createRow(3);
        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue("Operation Type");
        headerCell1.setCellStyle(headerStyle);

        Cell headerCell2 = headerRow.createCell(1);
        headerCell2.setCellValue("Count");
        headerCell2.setCellStyle(headerStyle);

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

        // Add data rows
        int rowNum = 4;
        for (Map.Entry<String, Integer> entry : operationCounts.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        // Add total row
        Row totalRow = sheet.createRow(rowNum);
        totalRow.createCell(0).setCellValue("Total");
        totalRow.getCell(0).setCellStyle(headerStyle);
        totalRow.createCell(1).setCellValue(operationCounts.values().stream().mapToInt(Integer::intValue).sum());
        totalRow.getCell(1).setCellStyle(headerStyle);
    }

    private void createDetailsSheet(Workbook workbook, Sheet sheet) {
        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Create title row
        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Detailed History Entries");

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleRow.getCell(0).setCellStyle(titleStyle);

        // Merge cells for title
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        // Create column headers
        Row headerRow = sheet.createRow(1);
        String[] headers = {"Timestamp", "Action", "Summary", "Details"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Add data rows
        int rowNum = 2;
        for (String entry : allHistoryEntries) {
            HistoryEntryItem item = parseHistoryEntry(entry);
            if (item == null) continue;

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getTimestamp());
            row.createCell(1).setCellValue(item.getAction());
            row.createCell(2).setCellValue(item.getSummary());

            // Create a cell style with word wrap
            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);

            Cell detailsCell = row.createCell(3);
            detailsCell.setCellValue(formatEntryDetails(item.getFullEntry()));
            detailsCell.setCellStyle(wrapStyle);
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