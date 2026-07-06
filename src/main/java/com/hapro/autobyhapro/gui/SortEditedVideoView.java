package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.EditedVideoSortItemResult;
import com.hapro.autobyhapro.entity.EditedVideoSortJobResult;
import com.hapro.autobyhapro.service.EditedVideoSortService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SortEditedVideoView {

    private final EditedVideoSortService editedVideoSortService =
            new EditedVideoSortService();

    private final ScrollPane rootScrollPane = new ScrollPane();
    private final VBox root = new VBox(14);

    private final TableView<EditedVideoSortItemResult> tableView = new TableView<>();
    private final TextArea resultArea = new TextArea();
    private final Label statusLabel = new Label("Sẵn sàng.");
    private final Label folderInfoLabel = new Label();

    private final ObservableList<EditedVideoSortItemResult> tableItems =
            FXCollections.observableArrayList();

    public SortEditedVideoView() {
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

        Label titleLabel = new Label("Phân loại video edit");
        titleLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(
                "Đọc video export từ CapCut, tự chuyển vào đúng folder edited theo batch/video đã tải."
        );
        descriptionLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #4b5563;
                """);

        VBox folderCard = buildFolderCard();
        VBox tableCard = buildTableCard();
        VBox resultCard = buildResultCard();

        root.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                folderCard,
                tableCard,
                resultCard
        );
    }

    private VBox buildFolderCard() {
        VBox card = card();

        Label cardTitle = cardTitle("Folder sử dụng");

        folderInfoLabel.setWrapText(true);
        folderInfoLabel.setStyle("""
                -fx-font-family: Consolas;
                -fx-font-size: 13px;
                -fx-text-fill: #374151;
                """);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button refreshButton = secondaryButton("Refresh thông tin");
        refreshButton.setOnAction(event -> refreshFolderInfo());

        Button openCapcutButton = secondaryButton("Mở capcut_export");
        openCapcutButton.setOnAction(event -> openFolder(AppPaths.CAPCUT_EXPORT_DIR));

        Button openEditedButton = secondaryButton("Mở edited");
        openEditedButton.setOnAction(event -> openFolder(AppPaths.EDITED_DIR));

        Button openUnknownButton = secondaryButton("Mở edited_unknown");
        openUnknownButton.setOnAction(event -> openFolder(AppPaths.EDITED_UNKNOWN_DIR));

        buttonBox.getChildren().addAll(
                refreshButton,
                openCapcutButton,
                openEditedButton,
                openUnknownButton
        );

        Label noteLabel = note(
                "Khi export từ CapCut, nên giữ tên file gần giống tên RAW. Tool nhận diện theo VID và batch code trong tên file."
        );

        card.getChildren().addAll(
                cardTitle,
                folderInfoLabel,
                buttonBox,
                noteLabel
        );

        return card;
    }

    private VBox buildTableCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("Kết quả phân loại");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button sortButton = primaryButton("Phân loại video trong capcut_export");
        sortButton.setOnAction(event -> sortEditedVideos(sortButton));

        Button clearButton = secondaryButton("Xóa bảng");
        clearButton.setOnAction(event -> {
            tableItems.clear();
            resultArea.clear();
            statusLabel.setText("Đã xóa bảng kết quả.");
        });

        headerBox.getChildren().addAll(
                cardTitle,
                spacer,
                sortButton,
                clearButton
        );

        buildTableColumns();

        tableView.setItems(tableItems);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.setPlaceholder(new Label("Chưa có kết quả phân loại."));
        tableView.setPrefHeight(360);

        card.getChildren().addAll(
                headerBox,
                tableView
        );

        return card;
    }

    private VBox buildResultCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("Log chi tiết");

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
        resultArea.setPrefHeight(240);
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

    private void buildTableColumns() {
        TableColumn<EditedVideoSortItemResult, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getStatus()))
        );
        statusColumn.setPrefWidth(110);

        TableColumn<EditedVideoSortItemResult, String> videoIdColumn = new TableColumn<>("Video ID");
        videoIdColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getVideoId()))
        );
        videoIdColumn.setPrefWidth(130);

        TableColumn<EditedVideoSortItemResult, String> batchColumn = new TableColumn<>("Batch");
        batchColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getBatchCode()))
        );
        batchColumn.setPrefWidth(140);

        TableColumn<EditedVideoSortItemResult, String> sourceColumn = new TableColumn<>("File export");
        sourceColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(fileNameOnly(data.getValue().getSourceFilePath()))
        );
        sourceColumn.setPrefWidth(220);

        TableColumn<EditedVideoSortItemResult, String> destinationColumn = new TableColumn<>("File đích");
        destinationColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(fileNameOnly(data.getValue().getDestinationFilePath()))
        );
        destinationColumn.setPrefWidth(240);

        TableColumn<EditedVideoSortItemResult, String> messageColumn = new TableColumn<>("Thông báo");
        messageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getMessage()))
        );
        messageColumn.setPrefWidth(280);

        tableView.getColumns().setAll(
                statusColumn,
                videoIdColumn,
                batchColumn,
                sourceColumn,
                destinationColumn,
                messageColumn
        );
    }

    private void sortEditedVideos(Button sortButton) {
        statusLabel.setText("Đang phân loại video edit...");
        resultArea.setText("Đang đọc folder:\n"
                + AppPaths.CAPCUT_EXPORT_DIR.toAbsolutePath()
                + "\n\nKhông tắt tool trong lúc đang xử lý.");

        sortButton.setDisable(true);

        Task<EditedVideoSortJobResult> task = new Task<>() {
            @Override
            protected EditedVideoSortJobResult call() {
                return editedVideoSortService.sortEditedVideos();
            }
        };

        task.setOnSucceeded(event -> {
            EditedVideoSortJobResult result = task.getValue();

            tableItems.setAll(result.getItemResults());

            statusLabel.setText("Phân loại hoàn tất.");
            resultArea.setText(buildJobResultText(result));

            sortButton.setDisable(false);
            refreshFolderInfo();

            GuiAlert.info(
                    "Phân loại hoàn tất",
                    "Tổng file xử lý: " + result.getTotalCount()
                            + "\nThành công: " + result.getSuccessCount()
                            + "\nLỗi/bỏ qua: " + result.getFailedCount()
            );
        });

        task.setOnFailed(event -> {
            Throwable throwable = task.getException();

            statusLabel.setText("Phân loại thất bại.");
            sortButton.setDisable(false);

            if (throwable instanceof Exception exception) {
                resultArea.setText("Lỗi phân loại:\n" + exception.getMessage());
                GuiAlert.error("Phân loại thất bại", exception);
            } else {
                resultArea.setText("Lỗi phân loại không xác định.");
                GuiAlert.warning("Phân loại thất bại", "Lỗi không xác định.");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String buildJobResultText(EditedVideoSortJobResult result) {
        StringBuilder builder = new StringBuilder();

        builder.append("KẾT QUẢ PHÂN LOẠI")
                .append(System.lineSeparator());
        builder.append("==================")
                .append(System.lineSeparator());

        builder.append("Tổng file xử lý: ")
                .append(result.getTotalCount())
                .append(System.lineSeparator());

        builder.append("Thành công: ")
                .append(result.getSuccessCount())
                .append(System.lineSeparator());

        builder.append("Lỗi/bỏ qua: ")
                .append(result.getFailedCount())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        if (result.getItemResults().isEmpty()) {
            builder.append("Folder capcut_export không có file nào.")
                    .append(System.lineSeparator());

            return builder.toString();
        }

        for (EditedVideoSortItemResult item : result.getItemResults()) {
            builder.append("------------------------------------")
                    .append(System.lineSeparator());

            builder.append("Trạng thái: ")
                    .append(emptyToDash(item.getStatus()))
                    .append(System.lineSeparator());

            builder.append("Video ID: ")
                    .append(emptyToDash(item.getVideoId()))
                    .append(System.lineSeparator());

            builder.append("Batch: ")
                    .append(emptyToDash(item.getBatchCode()))
                    .append(System.lineSeparator());

            builder.append("Nguồn: ")
                    .append(emptyToDash(item.getSourceFilePath()))
                    .append(System.lineSeparator());

            if (item.getDestinationFilePath() != null) {
                builder.append("Đích: ")
                        .append(item.getDestinationFilePath())
                        .append(System.lineSeparator());
            }

            builder.append("Thông báo: ")
                    .append(emptyToDash(item.getMessage()))
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    private void refreshFolderInfo() {
        try {
            Files.createDirectories(AppPaths.CAPCUT_EXPORT_DIR);
            Files.createDirectories(AppPaths.EDITED_DIR);
            Files.createDirectories(AppPaths.EDITED_UNKNOWN_DIR);

            long capcutFileCount = countFiles(AppPaths.CAPCUT_EXPORT_DIR);
            long unknownFileCount = countFiles(AppPaths.EDITED_UNKNOWN_DIR);

            folderInfoLabel.setText(
                    "CapCut export: " + AppPaths.CAPCUT_EXPORT_DIR.toAbsolutePath()
                            + "\nEdited: " + AppPaths.EDITED_DIR.toAbsolutePath()
                            + "\nEdited unknown: " + AppPaths.EDITED_UNKNOWN_DIR.toAbsolutePath()
                            + "\n\nFile đang chờ trong capcut_export: " + capcutFileCount
                            + "\nFile chưa nhận diện trong edited_unknown: " + unknownFileCount
            );

            statusLabel.setText("Đã refresh thông tin folder.");

        } catch (Exception exception) {
            folderInfoLabel.setText("Không thể đọc thông tin folder:\n" + exception.getMessage());
            statusLabel.setText("Refresh folder thất bại.");
        }
    }

    private long countFiles(Path folder) {
        try {
            if (!Files.exists(folder)) {
                return 0;
            }

            try (var stream = Files.list(folder)) {
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

    private String fileNameOnly(String pathText) {
        if (pathText == null || pathText.isBlank()) {
            return "-";
        }

        try {
            return Path.of(pathText).getFileName().toString();
        } catch (Exception exception) {
            return pathText;
        }
    }

    private String emptyToDash(String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }

        return text;
    }
}