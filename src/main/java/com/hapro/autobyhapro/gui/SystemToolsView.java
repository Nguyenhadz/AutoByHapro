package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.DatabaseBackupInfo;
import com.hapro.autobyhapro.service.DatabaseBackupService;
import com.hapro.autobyhapro.service.SystemToolService;
import com.hapro.autobyhapro.service.YoutubeCookieCheckService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SystemToolsView {

    private static final String YT_DLP_EXE_DOWNLOAD_URL =
            "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";

    private static final String YT_DLP_RELEASE_PAGE_URL =
            "https://github.com/yt-dlp/yt-dlp/releases/latest";

    private static final String FFMPEG_ZIP_DOWNLOAD_URL =
            "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip";

    private static final String FFMPEG_RELEASE_PAGE_URL =
            "https://github.com/BtbN/FFmpeg-Builds/releases/latest";

    private final SystemToolService systemToolService = new SystemToolService();
    private final DatabaseBackupService databaseBackupService = new DatabaseBackupService();
    private final YoutubeCookieCheckService youtubeCookieCheckService =
            new YoutubeCookieCheckService();

    private final ScrollPane rootScrollPane = new ScrollPane();
    private final VBox root = new VBox(14);

    private final Label statusLabel = new Label("Sẵn sàng.");
    private final TextArea resultArea = new TextArea();
    private final TableView<DatabaseBackupInfo> backupTableView = new TableView<>();

    public SystemToolsView() {
        buildLayout();
        refreshBackupTable(false);
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

        Label titleLabel = new Label("Công cụ hệ thống");
        titleLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(
                "Kiểm tra yt-dlp, cookies YouTube, FFmpeg, database, backup DB và mở nhanh các thư mục hệ thống."
        );
        descriptionLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #4b5563;
                """);

        VBox ytDlpCard = buildYtDlpCard();
        VBox youtubeCookieCard = buildYoutubeCookieCard();
        VBox ffmpegCard = buildFfmpegCard();
        VBox databaseCard = buildDatabaseCard();
        VBox resultCard = buildResultCard();

        root.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                ytDlpCard,
                youtubeCookieCard,
                ffmpegCard,
                databaseCard,
                resultCard
        );
    }

    private VBox buildYtDlpCard() {
        VBox card = card();

        Label cardTitle = cardTitle("yt-dlp");
        Label noteLabel = note("Kiểm tra file local, so sánh với bản mới online, update hoặc mở thư mục tools để thay yt-dlp.exe thủ công.");
        Label pathLabel = smallPathLabel("Vị trí: " + AppPaths.ytDlpFile().toAbsolutePath());

        Button checkButton = primaryButton("Kiểm tra yt-dlp");
        checkButton.setOnAction(event -> runInBackground(
                "Đang kiểm tra yt-dlp...",
                () -> systemToolService.checkYtDlp()
        ));

        Button updateButton = primaryButton("Update yt-dlp");
        updateButton.setOnAction(event -> runInBackground(
                "Đang update yt-dlp...",
                () -> systemToolService.updateYtDlp()
        ));

        Button openToolsButton = secondaryButton("Mở thư mục tools");
        openToolsButton.setOnAction(event -> openFolder(
                AppPaths.TOOLS_DIR,
                "Đã mở thư mục tools:\n" + AppPaths.TOOLS_DIR.toAbsolutePath()
        ));

        Button openYtDlpParentButton = secondaryButton("Mở chỗ chứa yt-dlp.exe");
        openYtDlpParentButton.setOnAction(event -> openFolder(
                AppPaths.ytDlpFile().getParent(),
                "Đã mở thư mục chứa yt-dlp.exe:\n" + AppPaths.ytDlpFile().getParent().toAbsolutePath()
        ));

        Button openYtDlpDownloadButton = secondaryButton("Mở link tải yt-dlp.exe");
        openYtDlpDownloadButton.setOnAction(event -> openUrl(
                YT_DLP_EXE_DOWNLOAD_URL,
                "Đã mở link tải yt-dlp.exe:\n" + YT_DLP_EXE_DOWNLOAD_URL
        ));

        Button openYtDlpReleaseButton = secondaryButton("Mở release yt-dlp");
        openYtDlpReleaseButton.setOnAction(event -> openUrl(
                YT_DLP_RELEASE_PAGE_URL,
                "Đã mở trang release yt-dlp:\n" + YT_DLP_RELEASE_PAGE_URL
        ));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().addAll(
                checkButton,
                updateButton,
                openToolsButton,
                openYtDlpParentButton,
                openYtDlpDownloadButton,
                openYtDlpReleaseButton
        );

        card.getChildren().addAll(cardTitle, noteLabel, pathLabel, buttonBox);

        return card;
    }

    private VBox buildYoutubeCookieCard() {
        VBox card = card();

        Label cardTitle = cardTitle("YouTube cookies");
        Label noteLabel = note(
                "Kiểm tra file youtube_cookies.txt trước khi tải YouTube. Nếu lỗi robot, hết hạn hoặc chỉ thấy storyboard, hãy export lại cookies rồi thay file cũ."
        );
        Label pathLabel = smallPathLabel("Vị trí: " + AppPaths.YOUTUBE_COOKIES_FILE.toAbsolutePath());
        Label denoPathLabel = smallPathLabel("Deno: " + AppPaths.denoFile().toAbsolutePath());

        Button checkYoutubeCookiesButton = primaryButton("Kiểm tra cookies YouTube");
        checkYoutubeCookiesButton.setOnAction(event -> runInBackground(
                "Đang kiểm tra cookies YouTube...",
                () -> youtubeCookieCheckService.checkYoutubeCookies()
        ));

        Button openCookiesFolderButton = secondaryButton("Mở folder cookies");
        openCookiesFolderButton.setOnAction(event -> openFolder(
                AppPaths.COOKIES_DIR,
                "Đã mở folder cookies:\n" + AppPaths.COOKIES_DIR.toAbsolutePath()
        ));

        Button openCookieFileButton = secondaryButton("Mở file cookies");
        openCookieFileButton.setOnAction(event -> openFile(
                AppPaths.YOUTUBE_COOKIES_FILE,
                "Đã mở file cookies:\n" + AppPaths.YOUTUBE_COOKIES_FILE.toAbsolutePath()
        ));

        Button openDenoFolderButton = secondaryButton("Mở folder Deno");
        openDenoFolderButton.setOnAction(event -> openFolder(
                AppPaths.denoFile().getParent(),
                "Đã mở folder Deno:\n" + AppPaths.denoFile().getParent().toAbsolutePath()
        ));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().addAll(
                checkYoutubeCookiesButton,
                openCookiesFolderButton,
                openCookieFileButton,
                openDenoFolderButton
        );

        card.getChildren().addAll(
                cardTitle,
                noteLabel,
                pathLabel,
                denoPathLabel,
                buttonBox
        );

        return card;
    }

    private VBox buildFfmpegCard() {
        VBox card = card();

        Label cardTitle = cardTitle("FFmpeg");
        Label noteLabel = note("Kiểm tra file FFmpeg local và so sánh với build Windows mới online. Nếu có build mới, tool sẽ hiện link tải zip.");
        Label pathLabel = smallPathLabel("Vị trí: " + AppPaths.ffmpegFile().toAbsolutePath());
        Label downloadGuideLabel = note(
                "Nếu mở release FFmpeg thấy nhiều file zip: hãy tải file ffmpeg-master-latest-win64-gpl.zip. "
                        + "Không tải file có chữ shared, lgpl, linux, macos hoặc arm64."
        );

        Button checkButton = primaryButton("Kiểm tra FFmpeg");
        checkButton.setOnAction(event -> runInBackground(
                "Đang kiểm tra FFmpeg...",
                () -> systemToolService.checkFfmpeg()
        ));

        Button openButton = secondaryButton("Mở thư mục FFmpeg");
        openButton.setOnAction(event -> openFolder(
                AppPaths.ffmpegFile().getParent(),
                "Đã mở thư mục FFmpeg:\n" + AppPaths.ffmpegFile().getParent().toAbsolutePath()
        ));

        Button openFfmpegDownloadButton = secondaryButton("Mở link tải FFmpeg zip");
        openFfmpegDownloadButton.setOnAction(event -> openUrl(
                FFMPEG_ZIP_DOWNLOAD_URL,
                "Đã mở link tải FFmpeg zip:\n" + FFMPEG_ZIP_DOWNLOAD_URL
        ));

        Button openFfmpegReleaseButton = secondaryButton("Mở release FFmpeg");
        openFfmpegReleaseButton.setOnAction(event -> openUrl(
                FFMPEG_RELEASE_PAGE_URL,
                "Đã mở trang release FFmpeg:\n"
                        + FFMPEG_RELEASE_PAGE_URL
                        + "\n\nNếu thấy nhiều file zip, hãy tải đúng file:\n"
                        + "ffmpeg-master-latest-win64-gpl.zip"
                        + "\n\nKhông tải file có chữ:\n"
                        + "- shared\n"
                        + "- lgpl\n"
                        + "- linux\n"
                        + "- macos\n"
                        + "- arm64"
        ));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().addAll(
                checkButton,
                openButton,
                openFfmpegDownloadButton,
                openFfmpegReleaseButton
        );

        card.getChildren().addAll(
                cardTitle,
                noteLabel,
                pathLabel,
                downloadGuideLabel,
                buttonBox
        );

        return card;
    }

    private VBox buildDatabaseCard() {
        VBox card = card();

        Label cardTitle = cardTitle("Database / Backup");
        Label noteLabel = note("Mỗi lần backup, tool chỉ giữ lại file backup mới nhất. File backup cũ sẽ tự xóa.");
        Label pathLabel = smallPathLabel("Vị trí DB: " + AppPaths.databaseFile().toAbsolutePath());

        Button checkButton = primaryButton("Kiểm tra database");
        checkButton.setOnAction(event -> runInBackground(
                "Đang kiểm tra database...",
                () -> systemToolService.checkDatabase()
        ));

        Button backupButton = primaryButton("Backup database ngay");
        backupButton.setOnAction(event -> backupDatabaseNow());

        Button restoreButton = dangerButton("Khôi phục DB từ file .db");
        restoreButton.setOnAction(event -> chooseAndRestoreDatabase());

        Button openBackupFolderButton = secondaryButton("Mở folder backup");
        openBackupFolderButton.setOnAction(event -> openFolder(
                AppPaths.BACKUP_DATABASE_DIR,
                "Đã mở folder backup database:\n" + AppPaths.BACKUP_DATABASE_DIR.toAbsolutePath()
        ));

        Button refreshBackupButton = secondaryButton("Refresh backup");
        refreshBackupButton.setOnAction(event -> refreshBackupTable(true));

        Button checkAllButton = secondaryButton("Kiểm tra toàn bộ");
        checkAllButton.setOnAction(event -> runInBackground(
                "Đang kiểm tra toàn bộ hệ thống...",
                () -> systemToolService.checkAll()
        ));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().addAll(
                checkButton,
                backupButton,
                restoreButton,
                openBackupFolderButton,
                refreshBackupButton,
                checkAllButton
        );

        buildBackupTableColumns();

        backupTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        backupTableView.setPlaceholder(new Label("Chưa có file backup database."));
        backupTableView.setPrefHeight(70);
        backupTableView.setMaxHeight(70);

        card.getChildren().addAll(
                cardTitle,
                noteLabel,
                pathLabel,
                buttonBox,
                backupTableView
        );

        return card;
    }

    private VBox buildResultCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("Kết quả kiểm tra");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearButton = secondaryButton("Xóa kết quả");
        clearButton.setOnAction(event -> {
            resultArea.clear();
            setStatus("Đã xóa kết quả.");
        });

        headerBox.getChildren().addAll(cardTitle, spacer, clearButton);

        statusLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-text-fill: #2563eb;
                """);

        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefHeight(240);
        resultArea.setStyle("""
                -fx-font-family: Consolas;
                -fx-font-size: 13px;
                """);

        card.getChildren().addAll(headerBox, statusLabel, resultArea);

        return card;
    }

    private void buildBackupTableColumns() {
        TableColumn<DatabaseBackupInfo, String> fileColumn = new TableColumn<>("File backup");
        fileColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getFileName()))
        );
        fileColumn.setPrefWidth(300);

        TableColumn<DatabaseBackupInfo, String> sizeColumn = new TableColumn<>("Dung lượng");
        sizeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(formatFileSize(data.getValue().getFileSizeBytes()))
        );
        sizeColumn.setPrefWidth(100);

        TableColumn<DatabaseBackupInfo, String> timeColumn = new TableColumn<>("Thời gian");
        timeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getLastModifiedTime()))
        );
        timeColumn.setPrefWidth(160);

        backupTableView.getColumns().setAll(
                fileColumn,
                sizeColumn,
                timeColumn
        );
    }

    private void backupDatabaseNow() {
        runInBackground(
                "Đang backup database...",
                () -> {
                    DatabaseBackupInfo backupInfo = databaseBackupService.backupNow();

                    Platform.runLater(() -> refreshBackupTable(false));

                    return "Backup database thành công."
                            + "\n\nFile: " + backupInfo.getFileName()
                            + "\nĐường dẫn: " + backupInfo.getFilePath()
                            + "\nDung lượng: " + formatFileSize(backupInfo.getFileSizeBytes())
                            + "\nThời gian: " + backupInfo.getLastModifiedTime();
                }
        );
    }

    private void chooseAndRestoreDatabase() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file database backup để khôi phục");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SQLite database (*.db)", "*.db"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(
                rootScrollPane.getScene().getWindow()
        );

        if (selectedFile == null) {
            return;
        }

        Path selectedPath = selectedFile.toPath();

        boolean confirmed = confirmRestoreDatabase(selectedPath);

        if (!confirmed) {
            setStatus("Đã hủy khôi phục DB.");
            setResult("Đã hủy khôi phục database.");
            return;
        }

        restoreDatabaseFromFile(selectedPath);
    }

    private boolean confirmRestoreDatabase(Path selectedPath) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận khôi phục database");
        alert.setHeaderText("Khôi phục DB từ file backup?");
        alert.setContentText(
                "Tool sẽ thay database hiện tại bằng file này:\n\n"
                        + selectedPath.toAbsolutePath()
                        + "\n\nTrước khi thay, tool sẽ tự backup DB hiện tại."
                        + "\nSau khi khôi phục xong, m nên tắt tool và mở lại."
        );

        return alert.showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .isPresent();
    }

    private void restoreDatabaseFromFile(Path selectedPath) {
        runInBackground(
                "Đang khôi phục database...",
                () -> {
                    String result = databaseBackupService.restoreFromBackup(selectedPath);

                    Platform.runLater(() -> refreshBackupTable(false));

                    return result
                            + "\n\nDB hiện tại sau restore:"
                            + "\n" + AppPaths.databaseFile().toAbsolutePath()
                            + "\n\nQuan trọng: hãy tắt tool và mở lại để toàn bộ dữ liệu được load lại sạch.";
                }
        );
    }

    private void refreshBackupTable(boolean showResult) {
        try {
            List<DatabaseBackupInfo> backups = databaseBackupService.listBackups();
            backupTableView.setItems(FXCollections.observableArrayList(backups));

            if (showResult) {
                setStatus("Đã refresh danh sách backup.");
                setResult("Đã refresh danh sách backup.\nSố file backup: " + backups.size());
            }

        } catch (Exception exception) {
            setStatus("Refresh backup thất bại.");
            setResult("Lỗi refresh backup:\n" + exception.getMessage());
            GuiAlert.error("Không thể tải danh sách backup", exception);
        }
    }

    private void openUrl(String url, String resultMessage) {
        try {
            if (url == null || url.isBlank()) {
                GuiAlert.warning("Không có link", "Link đang bị trống.");
                return;
            }

            new ProcessBuilder(
                    "cmd",
                    "/c",
                    "start",
                    "",
                    url
            ).start();

            setStatus("Đã mở link.");
            setResult(resultMessage);

        } catch (IOException exception) {
            setStatus("Mở link thất bại.");
            setResult("Không thể mở link:\n" + exception.getMessage());
            GuiAlert.error("Không thể mở link", exception);
        }
    }

    private void openFolder(Path folder, String resultMessage) {
        try {
            if (folder == null) {
                GuiAlert.warning("Không có đường dẫn", "Đường dẫn folder đang bị trống.");
                return;
            }

            Files.createDirectories(folder);

            new ProcessBuilder(
                    "explorer.exe",
                    folder.toAbsolutePath().toString()
            ).start();

            setStatus("Đã mở folder.");
            setResult(resultMessage);

        } catch (IOException exception) {
            setStatus("Mở folder thất bại.");
            setResult("Không thể mở folder:\n" + exception.getMessage());
            GuiAlert.error("Không thể mở folder", exception);
        }
    }

    private void openFile(Path file, String resultMessage) {
        try {
            if (file == null) {
                GuiAlert.warning("Không có đường dẫn", "Đường dẫn file đang bị trống.");
                return;
            }

            if (!Files.exists(file)) {
                GuiAlert.warning(
                        "Chưa có file",
                        "Chưa tìm thấy file:\n" + file.toAbsolutePath()
                                + "\n\nHãy bấm “Mở folder cookies”, rồi đặt file youtube_cookies.txt vào đó."
                );
                setStatus("Chưa có file cookies.");
                setResult("Chưa tìm thấy file:\n" + file.toAbsolutePath());
                return;
            }

            new ProcessBuilder(
                    "cmd",
                    "/c",
                    "start",
                    "",
                    file.toAbsolutePath().toString()
            ).start();

            setStatus("Đã mở file.");
            setResult(resultMessage);

        } catch (IOException exception) {
            setStatus("Mở file thất bại.");
            setResult("Không thể mở file:\n" + exception.getMessage());
            GuiAlert.error("Không thể mở file", exception);
        }
    }

    private void runInBackground(String startMessage, ToolAction action) {
        setStatus(startMessage);
        setResult(startMessage);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return action.run();
            }
        };

        task.setOnSucceeded(event -> {
            String result = task.getValue();

            setStatus("Hoàn thành.");
            setResult(result);
        });

        task.setOnFailed(event -> {
            Throwable throwable = task.getException();

            setStatus("Thao tác thất bại.");

            if (throwable instanceof Exception exception) {
                setResult("Lỗi:\n" + exception.getMessage());
                GuiAlert.error("Thao tác thất bại", exception);
            } else {
                setResult("Lỗi không xác định.");
                GuiAlert.warning("Thao tác thất bại", "Lỗi không xác định.");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    private void setResult(String text) {
        if (text == null) {
            text = "";
        }

        String finalText = text;

        Platform.runLater(() -> resultArea.setText(finalText));
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

    private Label smallPathLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("""
                -fx-font-size: 12px;
                -fx-text-fill: #374151;
                -fx-font-family: Consolas;
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

    private Button dangerButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(34);
        button.setStyle("""
            -fx-background-color: #dc2626;
            -fx-text-fill: white;
            -fx-font-size: 13px;
            -fx-font-weight: bold;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            """);
        return button;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        double kb = bytes / 1024.0;

        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }

        double mb = kb / 1024.0;

        return String.format("%.2f MB", mb);
    }

    private String nullToEmpty(String text) {
        if (text == null) {
            return "";
        }

        return text;
    }

    private interface ToolAction {
        String run();
    }
}
