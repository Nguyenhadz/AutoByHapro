package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.config.AppPaths;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HomeView {

    private final ScrollPane rootScrollPane = new ScrollPane();
    private final VBox root = new VBox(14);

    private final Label statusLabel = new Label("Sẵn sàng.");
    private final TextArea summaryArea = new TextArea();

    public HomeView() {
        buildLayout();
        refreshSummary();
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

        Label titleLabel = new Label("Auto By Hapro");
        titleLabel.setStyle("""
                -fx-font-size: 32px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(
                "Tool hỗ trợ tải video, phân loại video edit, upload thủ công và dọn file sau upload."
        );
        descriptionLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #4b5563;
                """);

        VBox workflowCard = buildWorkflowCard();
        VBox folderCard = buildFolderStatusCard();
        VBox noteCard = buildNoteCard();

        root.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                workflowCard,
                folderCard,
                noteCard
        );
    }

    private VBox buildWorkflowCard() {
        VBox card = card();

        Label cardTitle = cardTitle("Quy trình sử dụng");

        Label stepLabel = new Label("""
                1. Kế hoạch download: chọn fanpage/source và tải video mới.
                2. Edit thủ công bằng CapCut.
                3. Export video từ CapCut vào folder capcut_export.
                4. Phân loại video edit: tool chuyển video vào đúng folder edited.
                5. Upload thủ công: mở folder edited và upload lên Facebook/Meta.
                6. Mark as uploaded: đánh dấu batch đã upload.
                7. Xóa file + dọn DB: xóa raw/edited sau upload, vẫn giữ video ID chống tải trùng.
                """);

        stepLabel.setWrapText(true);
        stepLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #374151;
                -fx-line-spacing: 4;
                """);

        card.getChildren().addAll(
                cardTitle,
                stepLabel
        );

        return card;
    }

    private VBox buildFolderStatusCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("Tình trạng folder");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = primaryButton("Refresh");
        refreshButton.setOnAction(event -> refreshSummary());

        Button openRootButton = secondaryButton("Mở folder project");
        openRootButton.setOnAction(event -> openFolder(AppPaths.rootDir()));

        Button openRawButton = secondaryButton("Mở raw");
        openRawButton.setOnAction(event -> openFolder(AppPaths.RAW_DIR));

        Button openExportButton = secondaryButton("Mở capcut_export");
        openExportButton.setOnAction(event -> openFolder(AppPaths.CAPCUT_EXPORT_DIR));

        Button openEditedButton = secondaryButton("Mở edited");
        openEditedButton.setOnAction(event -> openFolder(AppPaths.EDITED_DIR));

        headerBox.getChildren().addAll(
                cardTitle,
                spacer,
                refreshButton,
                openRootButton,
                openRawButton,
                openExportButton,
                openEditedButton
        );

        statusLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-text-fill: #2563eb;
                """);

        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.setPrefHeight(260);
        summaryArea.setStyle("""
                -fx-font-family: Consolas;
                -fx-font-size: 13px;
                """);

        card.getChildren().addAll(
                headerBox,
                statusLabel,
                summaryArea
        );

        return card;
    }

    private VBox buildNoteCard() {
        VBox card = card();

        Label cardTitle = cardTitle("Ghi chú quan trọng");

        Label noteLabel = new Label("""
                - Video được lưu ID vào database ngay sau khi tải thành công.
                - Dù video có được edit/upload hay bị xóa khỏi folder, ID vẫn được giữ để chống tải trùng.
                - Sau khi upload xong, dùng Mark as uploaded rồi cleanup để xóa file raw/edited.
                - Folder edited_unknown là nơi chứa file edit chưa nhận diện được, cần kiểm tra lại tên file.
                - Database nằm trong folder data, nên backup trước khi đóng gói hoặc chuyển máy.
                """);

        noteLabel.setWrapText(true);
        noteLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #374151;
                -fx-line-spacing: 4;
                """);

        card.getChildren().addAll(
                cardTitle,
                noteLabel
        );

        return card;
    }

    private void refreshSummary() {
        try {
            Files.createDirectories(AppPaths.DATA_DIR);
            Files.createDirectories(AppPaths.RAW_DIR);
            Files.createDirectories(AppPaths.CAPCUT_EXPORT_DIR);
            Files.createDirectories(AppPaths.EDITED_DIR);
            Files.createDirectories(AppPaths.EDITED_UNKNOWN_DIR);
            Files.createDirectories(AppPaths.BACKUP_DATABASE_DIR);
            Files.createDirectories(AppPaths.EXPORTS_DIR);

            String text = "PROJECT ROOT"
                    + "\n" + AppPaths.rootDir().toAbsolutePath()
                    + "\n\nDATABASE"
                    + "\n" + AppPaths.databaseFile().toAbsolutePath()
                    + "\nDatabase tồn tại: " + yesNo(Files.exists(AppPaths.databaseFile()))
                    + "\n\nFOLDER STATUS"
                    + "\nraw: " + countFilesRecursive(AppPaths.RAW_DIR) + " file"
                    + "\ncapcut_export: " + countFilesRecursive(AppPaths.CAPCUT_EXPORT_DIR) + " file"
                    + "\nedited: " + countFilesRecursive(AppPaths.EDITED_DIR) + " file"
                    + "\nedited_unknown: " + countFilesRecursive(AppPaths.EDITED_UNKNOWN_DIR) + " file"
                    + "\nexports: " + countFilesRecursive(AppPaths.EXPORTS_DIR) + " file"
                    + "\nbackup database: " + countFilesRecursive(AppPaths.BACKUP_DATABASE_DIR) + " file"
                    + "\n\nTOOLS"
                    + "\nyt-dlp.exe: " + yesNo(Files.exists(AppPaths.ytDlpFile()))
                    + "\nffmpeg.exe: " + yesNo(Files.exists(AppPaths.ffmpegFile()));

            summaryArea.setText(text);
            statusLabel.setText("Đã refresh tình trạng folder.");

        } catch (Exception exception) {
            statusLabel.setText("Refresh thất bại.");
            summaryArea.setText("Không thể refresh folder:\n" + exception.getMessage());
        }
    }

    private long countFilesRecursive(Path folder) {
        try {
            if (!Files.exists(folder)) {
                return 0;
            }

            try (var stream = Files.walk(folder)) {
                return stream
                        .filter(Files::isRegularFile)
                        .count();
            }

        } catch (Exception exception) {
            return 0;
        }
    }

    private void openFolder(Path folder) {
        try {
            Files.createDirectories(folder);

            new ProcessBuilder(
                    "explorer.exe",
                    folder.toAbsolutePath().toString()
            ).start();

            statusLabel.setText("Đã mở folder.");
            summaryArea.setText("Đã mở folder:\n" + folder.toAbsolutePath());

        } catch (IOException exception) {
            statusLabel.setText("Mở folder thất bại.");
            summaryArea.setText("Không thể mở folder:\n" + exception.getMessage());
            GuiAlert.error("Không thể mở folder", exception);
        }
    }

    private String yesNo(boolean value) {
        return value ? "Có" : "Không";
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
}
