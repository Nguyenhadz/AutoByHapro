package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.license.LicenseCheckResult;
import com.hapro.autobyhapro.license.LicenseInfo;
import com.hapro.autobyhapro.license.LicenseManager;
import com.hapro.autobyhapro.license.MachineIdUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LicenseView {

    private final Runnable onLicenseActivated;

    private final ScrollPane rootScrollPane = new ScrollPane();
    private final VBox root = new VBox(14);

    private final Label statusLabel = new Label("Đang kiểm tra licence...");
    private final TextArea machineIdArea = new TextArea();
    private final TextArea resultArea = new TextArea();

    public LicenseView(Runnable onLicenseActivated) {
        this.onLicenseActivated = onLicenseActivated;
        buildLayout();
        refreshLicenseStatus();
    }

    public Parent getRoot() {
        return rootScrollPane;
    }

    private void buildLayout() {
        rootScrollPane.setFitToWidth(true);
        rootScrollPane.setStyle("-fx-background-color: transparent;");
        rootScrollPane.setContent(root);

        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: transparent;");

        Label titleLabel = new Label("Kích hoạt licence");
        titleLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(
                "Mỗi máy tính cần một licence riêng. Copy mã máy gửi cho admin để tạo file licence."
        );
        descriptionLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #4b5563;
                """);

        VBox machineCard = buildMachineCard();
        VBox licenseCard = buildLicenseCard();
        VBox resultCard = buildResultCard();

        root.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                machineCard,
                licenseCard,
                resultCard
        );
    }

    private VBox buildMachineCard() {
        VBox card = card();

        Label cardTitle = cardTitle("Mã máy hiện tại");

        machineIdArea.setEditable(false);
        machineIdArea.setWrapText(true);
        machineIdArea.setPrefHeight(72);
        machineIdArea.setStyle("""
                -fx-font-family: Consolas;
                -fx-font-size: 15px;
                """);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button copyButton = primaryButton("Copy mã máy");
        copyButton.setOnAction(event -> copyMachineId());

        Button refreshButton = secondaryButton("Refresh");
        refreshButton.setOnAction(event -> refreshLicenseStatus());

        buttonBox.getChildren().addAll(
                copyButton,
                refreshButton
        );

        Label noteLabel = note(
                "M gửi mã máy này cho admin để tạo licence 30 ngày, 1 năm hoặc vĩnh viễn."
        );

        card.getChildren().addAll(
                cardTitle,
                machineIdArea,
                buttonBox,
                noteLabel
        );

        return card;
    }

    private VBox buildLicenseCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("File licence");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button importButton = primaryButton("Import licence");
        importButton.setOnAction(event -> importLicense());

        Button openDataButton = secondaryButton("Mở folder data");
        openDataButton.setOnAction(event -> openFolder(AppPaths.DATA_DIR));

        Button continueButton = secondaryButton("Tiếp tục vào tool");
        continueButton.setOnAction(event -> continueIfValid());

        headerBox.getChildren().addAll(
                cardTitle,
                spacer,
                importButton,
                openDataButton,
                continueButton
        );

        Label pathLabel = new Label(
                "Licence sẽ được lưu tại:\n" + LicenseManager.getLicenseFile().toAbsolutePath()
        );
        pathLabel.setWrapText(true);
        pathLabel.setStyle("""
                -fx-font-family: Consolas;
                -fx-font-size: 13px;
                -fx-text-fill: #374151;
                """);

        Label noteLabel = note(
                "Không sửa nội dung file licence. Nếu sửa, chữ ký sẽ sai và tool sẽ khóa lại."
        );

        card.getChildren().addAll(
                headerBox,
                pathLabel,
                noteLabel
        );

        return card;
    }

    private VBox buildResultCard() {
        VBox card = card();

        Label cardTitle = cardTitle("Trạng thái licence");

        statusLabel.setStyle("""
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                -fx-text-fill: #2563eb;
                """);

        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefHeight(260);
        resultArea.setStyle("""
                -fx-font-family: Consolas;
                -fx-font-size: 13px;
                """);

        card.getChildren().addAll(
                cardTitle,
                statusLabel,
                resultArea
        );

        return card;
    }

    private void refreshLicenseStatus() {
        String machineId = MachineIdUtil.getMachineId();
        machineIdArea.setText(machineId);

        LicenseCheckResult result = LicenseManager.checkLicense();

        if (result.isValid()) {
            statusLabel.setText("Licence hợp lệ.");
            statusLabel.setStyle("""
                    -fx-font-size: 15px;
                    -fx-font-weight: bold;
                    -fx-text-fill: #16a34a;
                    """);
        } else {
            statusLabel.setText("Licence chưa hợp lệ: " + result.getStatus());
            statusLabel.setStyle("""
                    -fx-font-size: 15px;
                    -fx-font-weight: bold;
                    -fx-text-fill: #dc2626;
                    """);
        }

        resultArea.setText(buildLicenseText(result));
    }

    private void copyMachineId() {
        String machineId = MachineIdUtil.getMachineId();

        ClipboardContent content = new ClipboardContent();
        content.putString(machineId);
        Clipboard.getSystemClipboard().setContent(content);

        statusLabel.setText("Đã copy mã máy.");
        resultArea.setText("Đã copy mã máy:\n" + machineId);
    }

    private void importLicense() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file licence");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Licence file", "*.lic", "*.json", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(
                rootScrollPane.getScene().getWindow()
        );

        if (selectedFile == null) {
            return;
        }

        LicenseCheckResult result = LicenseManager.importLicense(selectedFile.toPath());

        refreshLicenseStatus();

        if (result.isValid()) {
            GuiAlert.info(
                    "Kích hoạt thành công",
                    "Licence hợp lệ. Tool đã được mở khóa."
            );

            if (onLicenseActivated != null) {
                onLicenseActivated.run();
            }

        } else {
            GuiAlert.warning(
                    "Licence chưa hợp lệ",
                    result.getMessage()
            );
        }
    }

    private void continueIfValid() {
        LicenseCheckResult result = LicenseManager.checkLicense();

        if (!result.isValid()) {
            GuiAlert.warning(
                    "Licence chưa hợp lệ",
                    result.getMessage()
            );
            refreshLicenseStatus();
            return;
        }

        if (onLicenseActivated != null) {
            onLicenseActivated.run();
        }
    }

    private String buildLicenseText(LicenseCheckResult result) {
        StringBuilder builder = new StringBuilder();

        builder.append("TRẠNG THÁI")
                .append("\n==========")
                .append("\nStatus: ")
                .append(result.getStatus())
                .append("\nThông báo: ")
                .append(result.getMessage())
                .append("\n\nMÁY HIỆN TẠI")
                .append("\n============")
                .append("\nMachine ID: ")
                .append(MachineIdUtil.getMachineId())
                .append("\n\nFILE LICENCE")
                .append("\n============")
                .append("\n")
                .append(LicenseManager.getLicenseFile().toAbsolutePath())
                .append("\nTồn tại: ")
                .append(Files.exists(LicenseManager.getLicenseFile()) ? "Có" : "Không");

        LicenseInfo info = result.getLicenseInfo();

        if (info != null) {
            builder.append("\n\nTHÔNG TIN LICENCE")
                    .append("\n=================")
                    .append("\nApp: ")
                    .append(emptyToDash(info.getAppCode()))
                    .append("\nKhách hàng: ")
                    .append(emptyToDash(info.getCustomerName()))
                    .append("\nLoại licence: ")
                    .append(emptyToDash(info.getLicenseType()))
                    .append("\nNgày cấp: ")
                    .append(emptyToDash(info.getIssuedAt()))
                    .append("\nNgày hết hạn: ")
                    .append(info.isLifetime() ? "Vĩnh viễn" : emptyToDash(info.getExpiresAt()))
                    .append("\nMachine ID trong licence: ")
                    .append(emptyToDash(info.getMachineId()));
        }

        return builder.toString();
    }

    private void openFolder(Path folder) {
        try {
            Files.createDirectories(folder);

            new ProcessBuilder(
                    "explorer.exe",
                    folder.toAbsolutePath().toString()
            ).start();

            statusLabel.setText("Đã mở folder.");
            resultArea.setText("Đã mở folder:\n" + folder.toAbsolutePath());

        } catch (IOException exception) {
            GuiAlert.error("Không thể mở folder", exception);
        }
    }

    private VBox card() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 12;
                -fx-border-color: #e5e7eb;
                -fx-border-radius: 12;
                """);
        return card;
    }

    private Label cardTitle(String text) {
        Label label = new Label(text);
        label.setStyle("""
                -fx-font-size: 18px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);
        return label;
    }

    private Label note(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("""
                -fx-font-size: 13px;
                -fx-text-fill: #6b7280;
                """);
        return label;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(34);
        button.setStyle("""
                -fx-background-color: #2563eb;
                -fx-text-fill: white;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                """);
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(34);
        button.setStyle("""
                -fx-background-color: #e5e7eb;
                -fx-text-fill: #111827;
                -fx-font-size: 13px;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                """);
        return button;
    }

    private String emptyToDash(String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }

        return text;
    }
}
