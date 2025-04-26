package com.controllers.nour;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;



import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class QRCodeViewController {

    @FXML
    private ImageView qrCodeImageView;
    @FXML
    private TextArea orderDetailsTextArea;
    @FXML
    private Button closeButton;

    public void generateQRCode(String content) {
        try {
            orderDetailsTextArea.setText(content);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 250, 250, hints);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            WritableImage writableImage = SwingFXUtils.toFXImage(bufferedImage, null);

            qrCodeImageView.setImage(writableImage);

        } catch (WriterException e) {
            e.printStackTrace();
            orderDetailsTextArea.setText("Error generating QR code: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }
}