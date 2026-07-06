package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.FileSyncResult;
import com.hapro.autobyhapro.service.FileSyncService;
import javafx.concurrent.Task;
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

public class FileSyncView {

    private final FileSyncService fileSyncService = new FileSyncService();

    private final ScrollPane rootScrollPane = new ScrollPane();
    private final VBox root = new VBox(14);

    private final Label statusLabel = new Label("Sẵn sàng.");
    private final Label folderInfoLabel = new Label();
    private final TextArea resultArea = new TextArea();

    public FileSyncView() {
        buildLayout();
        refreshFolderInfo();
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

        Label titleLabel = new Label("Đồng bộ file");
        titleLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(
                "Kiểm tra đường dẫn file trong DB còn tồn tại thật trên ổ cứng không, rồi cập nhật lại trạng thái video/batch."
        );
        descriptionLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #4b5563;
                """);

        VBox folderCard = buildFolderCard();
        VBox syncCard = buildSyncCard();
        VBox resultCard = buildResultCard();

        root.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                folderCard,
                syncCard,
                resultCard
        );
    }

    private VBox buildFolderCard() {
        VBox card = card();

        Label cardTitle = cardTitle("Folder cần kiểm tra");

        folderInfoLabel.setWrapText(true);
        folderInfoLabel.setStyle("""
                -fx-font-family: Consolas;
                -fx-font-size: 13px;
                -fx-text-fill: #374151;
                """);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button refreshButton = secondaryButton("Refresh folder");
        refreshButton.setOnAction(event -> refreshFolderInfo());

        Button openRawButton = secondaryButton("Mở raw");
        openRawButton.setOnAction(event -> openRootFolder(AppPaths.RAW_DIR));

        Button openCapcutButton = secondaryButton("Mở capcut_export");
        openCapcutButton.setOnAction(event -> openRootFolder(AppPaths.CAPCUT_EXPORT_DIR));

        Button openEditedButton = secondaryButton("Mở edited");
        openEditedButton.setOnAction(event -> openRootFolder(AppPaths.EDITED_DIR));

        Button openUnknownButton = secondaryButton("Mở edited_unknown");
        openUnknownButton.setOnAction(event -> openRootFolder(AppPaths.EDITED_UNKNOWN_DIR));

        buttonBox.getChildren().addAll(
                refreshButton,
                openRawButton,
                openCapcutButton,
                openEditedButton,
                openUnknownButton
        );

        card.getChildren().addAll(
                cardTitle,
                folderInfoLabel,
                buttonBox
        );

        return card;
    }

    private VBox buildSyncCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("Đồng bộ DB với file thật");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button syncButton = primaryButton("Bắt đầu đồng bộ");
        syncButton.setOnAction(event -> syncDatabaseWithRealFiles(syncButton));

        headerBox.getChildren().addAll(
                cardTitle,
                spacer,
                syncButton
        );

        Label noteLabel = note(
                "Chức năng này không xóa file. Nó chỉ kiểm tra file trong DB còn tồn tại thật không, rồi cập nhật trạng thái video/batch cho đúng."
        );

        card.getChildren().addAll(
                headerBox,
                noteLabel
        );

        return card;
    }

    private VBox buildResultCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("Kết quả đồng bộ");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearButton = secondaryButton("Xóa log");
        clearButton.setOnAction(event -> {
            resultArea.clear();
            statusLabel.setText("Đã xóa log.");
        });

        headerBox.getChildren().addAll(
                cardTitle,
                spacer,
                clearButton
        );

        statusLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-text-fill: #2563eb;
                """);

        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefHeight(330);
        resultArea.setStyle("""
                -fx-font-family: Consolas;
                -fx-font-size: 13px;
                """);

        card.getChildren().addAll(
                headerBox,
                statusLabel,
                resultArea
        );

        return card;
    }

    private void syncDatabaseWithRealFiles(Button syncButton) {
        syncButton.setDisable(true);
        statusLabel.setText("Đang đồng bộ DB với file thật...");
        resultArea.setText("Đang kiểm tra video_files trong DB...\n"
                + "Bước này không xóa file.\n\n"
                + "Không tắt tool trong lúc đang xử lý.");

        Task<FileSyncResult> task = new Task<>() {
            @Override
            protected FileSyncResult call() {
                return fileSyncService.syncDatabaseWithRealFiles();
            }
        };

        task.setOnSucceeded(event -> {
            FileSyncResult result = task.getValue();

            syncButton.setDisable(false);
            statusLabel.setText("Đồng bộ hoàn tất.");
            resultArea.setText(buildResultText(result));
            refreshFolderInfo();

            GuiAlert.info(
                    "Đồng bộ hoàn tất",
                    "File còn tồn tại: " + result.getActualExistingFileCount()
                            + "\nFile đã mất: " + result.getActualMissingFileCount()
                            + "\nRecord đã cập nhật: " + result.getUpdatedFileRecordCount()
                            + "\nBatch đổi trạng thái: " + result.getVideoBatchStatusUpdatedCount()
            );
        });

        task.setOnFailed(event -> {
            syncButton.setDisable(false);

            Throwable throwable = task.getException();
            statusLabel.setText("Đồng bộ thất bại.");

            if (throwable instanceof Exception exception) {
                resultArea.setText("Lỗi đồng bộ:\n" + exception.getMessage());
                GuiAlert.error("Đồng bộ thất bại", exception);
            } else {
                resultArea.setText("Lỗi đồng bộ không xác định.");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String buildResultText(FileSyncResult result) {
        return "KẾT QUẢ ĐỒNG BỘ FILE"
                + "\n====================="
                + "\nTổng record video_files trong DB: " + result.getTotalFileRecordCount()
                + "\nFile thật còn tồn tại: " + result.getActualExistingFileCount()
                + "\nFile thật đã mất: " + result.getActualMissingFileCount()
                + "\nRecord video_files đã cập nhật file_exists: " + result.getUpdatedFileRecordCount()
                + "\n\nVideo READY_UPLOAD nhưng mất file edit, đổi về DOWNLOADED: "
                + result.getVideosChangedToDownloadedCount()
                + "\nVideo mất cả raw/edit, đổi sang FILE_MISSING: "
                + result.getVideosChangedToFileMissingCount()
                + "\nBatch đã cập nhật lại trạng thái: "
                + result.getVideoBatchStatusUpdatedCount()
                + "\n\nGIẢI THÍCH"
                + "\n- Chức năng này không xóa file."
                + "\n- FILE_MISSING vẫn giữ video_id để chống tải trùng."
                + "\n- Nếu muốn tải lại video bị mất file thì sau này mình làm thêm chức năng riêng.";
    }

    private void refreshFolderInfo() {
        try {
            Files.createDirectories(AppPaths.RAW_DIR);
            Files.createDirectories(AppPaths.CAPCUT_EXPORT_DIR);
            Files.createDirectories(AppPaths.EDITED_DIR);
            Files.createDirectories(AppPaths.EDITED_UNKNOWN_DIR);

            folderInfoLabel.setText(
                    "Raw: " + AppPaths.RAW_DIR.toAbsolutePath()
                            + "\nCapCut export: " + AppPaths.CAPCUT_EXPORT_DIR.toAbsolutePath()
                            + "\nEdited: " + AppPaths.EDITED_DIR.toAbsolutePath()
                            + "\nEdited unknown: " + AppPaths.EDITED_UNKNOWN_DIR.toAbsolutePath()
                            + "\n\nSố file raw: " + countFilesRecursive(AppPaths.RAW_DIR)
                            + "\nSố file capcut_export: " + countFilesRecursive(AppPaths.CAPCUT_EXPORT_DIR)
                            + "\nSố file edited: " + countFilesRecursive(AppPaths.EDITED_DIR)
                            + "\nSố file edited_unknown: " + countFilesRecursive(AppPaths.EDITED_UNKNOWN_DIR)
            );

            statusLabel.setText("Đã refresh folder.");

        } catch (Exception exception) {
            folderInfoLabel.setText("Không thể đọc folder:\n" + exception.getMessage());
            statusLabel.setText("Refresh folder thất bại.");
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

    private void openRootFolder(Path folder) {
        try {
            Files.createDirectories(folder);

            new ProcessBuilder(
                    "explorer.exe",
                    folder.toAbsolutePath().toString()
            ).start();

            statusLabel.setText("Đã mở folder.");
            resultArea.setText("Đã mở folder:\n" + folder.toAbsolutePath());

        } catch (IOException exception) {
            statusLabel.setText("Mở folder thất bại.");
            resultArea.setText("Không thể mở folder:\n" + exception.getMessage());
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
}
