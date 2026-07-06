package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.ManualUploadMarkResult;
import com.hapro.autobyhapro.entity.ReadyUploadBatch;
import com.hapro.autobyhapro.entity.UploadCleanupResult;
import com.hapro.autobyhapro.entity.UploadedMarkedBatch;
import com.hapro.autobyhapro.repository.ManualUploadRepository;
import com.hapro.autobyhapro.service.UploadCleanupService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ManualUploadView {

    private final ManualUploadRepository manualUploadRepository =
            new ManualUploadRepository();

    private final UploadCleanupService uploadCleanupService =
            new UploadCleanupService();

    private final ScrollPane rootScrollPane = new ScrollPane();
    private final VBox root = new VBox(14);

    private final TableView<ReadyUploadBatch> readyTableView = new TableView<>();
    private final TableView<UploadedMarkedBatch> uploadedTableView = new TableView<>();

    private final TextArea resultArea = new TextArea();
    private final Label statusLabel = new Label("Sẵn sàng.");

    private final ObservableList<ReadyUploadBatch> readyItems =
            FXCollections.observableArrayList();

    private final ObservableList<UploadedMarkedBatch> uploadedItems =
            FXCollections.observableArrayList();

    public ManualUploadView() {
        buildLayout();
        refreshAll();
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

        Label titleLabel = new Label("Upload thủ công");
        titleLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(
                "Mở folder edited để upload thủ công lên Facebook/Meta, sau đó đánh dấu đã upload và dọn file raw/edited."
        );
        descriptionLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #4b5563;
                """);

        VBox readyCard = buildReadyUploadCard();
        VBox uploadedCard = buildUploadedMarkedCard();
        VBox resultCard = buildResultCard();

        root.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                readyCard,
                uploadedCard,
                resultCard
        );
    }

    private VBox buildReadyUploadCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("Batch sẵn sàng upload");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = secondaryButton("Refresh");
        refreshButton.setOnAction(event -> refreshAll());

        Button openEditedRootButton = secondaryButton("Mở folder edited gốc");
        openEditedRootButton.setOnAction(event -> openOrCreateFolder(AppPaths.EDITED_DIR));

        Button openSelectedButton = secondaryButton("Mở folder đã chọn");
        openSelectedButton.setOnAction(event -> openSelectedReadyFolder());

        Button markUploadedButton = primaryButton("Mark as uploaded");
        markUploadedButton.setOnAction(event -> markSelectedReadyBatchUploaded(markUploadedButton));

        headerBox.getChildren().addAll(
                cardTitle,
                spacer,
                refreshButton,
                openEditedRootButton,
                openSelectedButton,
                markUploadedButton
        );

        Label noteLabel = note(
                "Chỉ bấm Mark as uploaded sau khi m đã upload xong toàn bộ video trong folder đó lên Facebook/Meta."
        );

        buildReadyTableColumns();

        readyTableView.setItems(readyItems);
        readyTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        readyTableView.setPlaceholder(new Label("Chưa có batch nào READY_UPLOAD."));
        readyTableView.setPrefHeight(260);

        card.getChildren().addAll(
                headerBox,
                noteLabel,
                readyTableView
        );

        return card;
    }

    private VBox buildUploadedMarkedCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("Batch đã upload chờ dọn");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = secondaryButton("Refresh");
        refreshButton.setOnAction(event -> refreshAll());

        Button openRawButton = secondaryButton("Mở raw");
        openRawButton.setOnAction(event -> openSelectedUploadedRawFolder());

        Button openEditedButton = secondaryButton("Mở edited");
        openEditedButton.setOnAction(event -> openSelectedUploadedEditedFolder());

        Button cleanupButton = dangerButton("Xóa file + dọn DB");
        cleanupButton.setOnAction(event -> cleanupSelectedUploadedBatch(cleanupButton));

        Button cleanupAllButton = dangerButton("Xóa toàn bộ đã upload chờ dọn");
        cleanupAllButton.setOnAction(event -> cleanupAllUploadedBatches(cleanupAllButton));

        headerBox.getChildren().addAll(
                cardTitle,
                spacer,
                refreshButton,
                openRawButton,
                openEditedButton,
                cleanupButton,
                cleanupAllButton
        );

        Label noteLabel = note(
                "Cleanup sẽ xóa file raw/edited của batch đã upload, xóa video_files trong DB, nhưng vẫn giữ video ID để chống tải trùng."
        );

        buildUploadedTableColumns();

        uploadedTableView.setItems(uploadedItems);
        uploadedTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        uploadedTableView.setPlaceholder(new Label("Chưa có batch nào UPLOADED_MARKED."));
        uploadedTableView.setPrefHeight(260);

        card.getChildren().addAll(
                headerBox,
                noteLabel,
                uploadedTableView
        );

        return card;
    }

    private VBox buildResultCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("Log thao tác");

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

    private void buildReadyTableColumns() {
        TableColumn<ReadyUploadBatch, String> pageColumn = new TableColumn<>("Page");
        pageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        emptyToDash(data.getValue().getPageCode())
                                + " - "
                                + emptyToDash(data.getValue().getPageName())
                )
        );
        pageColumn.setPrefWidth(180);

        TableColumn<ReadyUploadBatch, String> batchColumn = new TableColumn<>("Batch");
        batchColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getBatchCode()))
        );
        batchColumn.setPrefWidth(170);

        TableColumn<ReadyUploadBatch, Number> readyCountColumn = new TableColumn<>("Ready");
        readyCountColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getReadyVideoCount())
        );

        TableColumn<ReadyUploadBatch, Number> totalCountColumn = new TableColumn<>("Tổng");
        totalCountColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getTotalVideoCount())
        );

        TableColumn<ReadyUploadBatch, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getBatchStatus()))
        );

        TableColumn<ReadyUploadBatch, String> folderColumn = new TableColumn<>("Edited folder");
        folderColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getEditedFolderPath()))
        );
        folderColumn.setPrefWidth(300);

        readyTableView.getColumns().setAll(
                pageColumn,
                batchColumn,
                readyCountColumn,
                totalCountColumn,
                statusColumn,
                folderColumn
        );
    }

    private void buildUploadedTableColumns() {
        TableColumn<UploadedMarkedBatch, String> pageColumn = new TableColumn<>("Page");
        pageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        emptyToDash(data.getValue().getPageCode())
                                + " - "
                                + emptyToDash(data.getValue().getPageName())
                )
        );
        pageColumn.setPrefWidth(180);

        TableColumn<UploadedMarkedBatch, String> batchColumn = new TableColumn<>("Batch");
        batchColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getBatchCode()))
        );
        batchColumn.setPrefWidth(170);

        TableColumn<UploadedMarkedBatch, Number> uploadedCountColumn = new TableColumn<>("Uploaded");
        uploadedCountColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getUploadedMarkedVideoCount())
        );

        TableColumn<UploadedMarkedBatch, Number> totalCountColumn = new TableColumn<>("Tổng");
        totalCountColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getTotalVideoCount())
        );

        TableColumn<UploadedMarkedBatch, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getBatchStatus()))
        );

        TableColumn<UploadedMarkedBatch, String> editedFolderColumn = new TableColumn<>("Edited folder");
        editedFolderColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getEditedFolderPath()))
        );
        editedFolderColumn.setPrefWidth(260);

        uploadedTableView.getColumns().setAll(
                pageColumn,
                batchColumn,
                uploadedCountColumn,
                totalCountColumn,
                statusColumn,
                editedFolderColumn
        );
    }

    private void refreshAll() {
        try {
            List<ReadyUploadBatch> readyBatches = findUsableReadyUploadBatches();
            List<UploadedMarkedBatch> uploadedBatches =
                    manualUploadRepository.findUploadedMarkedBatches();

            readyItems.setAll(readyBatches);
            uploadedItems.setAll(uploadedBatches);

            statusLabel.setText("Đã refresh dữ liệu upload.");
            resultArea.setText(
                    "Đã refresh dữ liệu."
                            + "\nBatch sẵn sàng upload: " + readyBatches.size()
                            + "\nBatch đã upload chờ dọn: " + uploadedBatches.size()
            );

        } catch (Exception exception) {
            statusLabel.setText("Refresh thất bại.");
            resultArea.setText("Lỗi refresh:\n" + exception.getMessage());
            GuiAlert.error("Refresh upload thất bại", exception);
        }
    }

    private List<ReadyUploadBatch> findUsableReadyUploadBatches() {
        List<ReadyUploadBatch> allBatches = manualUploadRepository.findReadyUploadBatches();
        List<ReadyUploadBatch> usableBatches = new ArrayList<>();

        for (ReadyUploadBatch batch : allBatches) {
            Path folder = Path.of(batch.getEditedFolderPath());

            if (folderExistsAndHasVideo(folder)) {
                usableBatches.add(batch);
            }
        }

        return usableBatches;
    }

    private boolean folderExistsAndHasVideo(Path folder) {
        if (folder == null) {
            return false;
        }

        if (!Files.exists(folder)) {
            return false;
        }

        if (!Files.isDirectory(folder)) {
            return false;
        }

        try (var stream = Files.list(folder)) {
            return stream.anyMatch(path ->
                    Files.isRegularFile(path)
                            && isVideoFile(path.getFileName().toString())
            );

        } catch (IOException exception) {
            return false;
        }
    }

    private boolean isVideoFile(String fileName) {
        if (fileName == null) {
            return false;
        }

        String lower = fileName.toLowerCase();

        return lower.endsWith(".mp4")
                || lower.endsWith(".mov")
                || lower.endsWith(".mkv")
                || lower.endsWith(".avi")
                || lower.endsWith(".webm");
    }

    private void openSelectedReadyFolder() {
        ReadyUploadBatch selected = readyTableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            GuiAlert.warning("Chưa chọn batch", "M chọn một batch READY_UPLOAD trước.");
            return;
        }

        openExistingFolder(Path.of(selected.getEditedFolderPath()));
    }

    private void markSelectedReadyBatchUploaded(Button button) {
        ReadyUploadBatch selected = readyTableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            GuiAlert.warning("Chưa chọn batch", "M chọn batch đã upload xong trước.");
            return;
        }

        boolean confirmed = confirm(
                "Xác nhận đã upload?",
                "Chỉ xác nhận nếu m đã upload xong batch này lên Facebook/Meta.\n\n"
                        + selected.getPageCode()
                        + " - "
                        + selected.getPageName()
                        + "\nBatch: "
                        + selected.getBatchCode()
                        + "\nSố video READY_UPLOAD: "
                        + selected.getReadyVideoCount()
        );

        if (!confirmed) {
            return;
        }

        button.setDisable(true);
        statusLabel.setText("Đang đánh dấu uploaded...");

        Task<ManualUploadMarkResult> task = new Task<>() {
            @Override
            protected ManualUploadMarkResult call() {
                return manualUploadRepository.markBatchUploaded(selected);
            }
        };

        task.setOnSucceeded(event -> {
            ManualUploadMarkResult result = task.getValue();

            button.setDisable(false);
            refreshAll();

            statusLabel.setText("Đã đánh dấu uploaded.");
            resultArea.setText(buildMarkResultText(result));

            GuiAlert.info(
                    "Đã đánh dấu uploaded",
                    "Batch: " + result.getBatchCode()
                            + "\nSố video đã đánh dấu: " + result.getMarkedVideoCount()
                            + "\nStatus mới: " + result.getNewBatchStatus()
            );
        });

        task.setOnFailed(event -> {
            button.setDisable(false);

            Throwable throwable = task.getException();
            statusLabel.setText("Đánh dấu uploaded thất bại.");

            if (throwable instanceof Exception exception) {
                resultArea.setText("Lỗi mark uploaded:\n" + exception.getMessage());
                GuiAlert.error("Mark uploaded thất bại", exception);
            } else {
                resultArea.setText("Lỗi không xác định.");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void openSelectedUploadedRawFolder() {
        UploadedMarkedBatch selected = uploadedTableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            GuiAlert.warning("Chưa chọn batch", "M chọn một batch UPLOADED_MARKED trước.");
            return;
        }

        openExistingFolder(Path.of(selected.getRawFolderPath()));
    }

    private void openSelectedUploadedEditedFolder() {
        UploadedMarkedBatch selected = uploadedTableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            GuiAlert.warning("Chưa chọn batch", "M chọn một batch UPLOADED_MARKED trước.");
            return;
        }

        openExistingFolder(Path.of(selected.getEditedFolderPath()));
    }

    private void cleanupSelectedUploadedBatch(Button button) {
        UploadedMarkedBatch selected = uploadedTableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            GuiAlert.warning("Chưa chọn batch", "M chọn batch đã upload chờ dọn trước.");
            return;
        }

        boolean confirmed = confirm(
                "Xác nhận xóa file?",
                "Thao tác này sẽ xóa file raw/edited của batch đã upload và dọn DB.\n\n"
                        + selected.getPageCode()
                        + " - "
                        + selected.getPageName()
                        + "\nBatch: "
                        + selected.getBatchCode()
                        + "\nVideo đã mark uploaded: "
                        + selected.getUploadedMarkedVideoCount()
                        + "\n\nVideo ID vẫn được giữ lại để chống tải trùng."
        );

        if (!confirmed) {
            return;
        }

        button.setDisable(true);
        statusLabel.setText("Đang cleanup sau upload...");
        resultArea.setText("Đang xóa file raw/edited và dọn DB cho batch:\n"
                + selected.getBatchCode()
                + "\n\nKhông tắt tool trong lúc đang xử lý.");

        Task<UploadCleanupResult> task = new Task<>() {
            @Override
            protected UploadCleanupResult call() {
                return uploadCleanupService.cleanupUploadedBatch(selected);
            }
        };

        task.setOnSucceeded(event -> {
            UploadCleanupResult result = task.getValue();

            button.setDisable(false);
            refreshAll();

            statusLabel.setText("Cleanup hoàn tất.");
            resultArea.setText(buildCleanupResultText(result));

            GuiAlert.info(
                    "Cleanup hoàn tất",
                    "Batch: " + result.getBatchCode()
                            + "\nTrạng thái: " + result.getStatus()
                            + "\nRaw files xóa: " + result.getRawDeletedFileCount()
                            + "\nEdited files xóa: " + result.getEditedDeletedFileCount()
            );
        });

        task.setOnFailed(event -> {
            button.setDisable(false);

            Throwable throwable = task.getException();
            statusLabel.setText("Cleanup thất bại.");

            if (throwable instanceof Exception exception) {
                resultArea.setText("Lỗi cleanup:\n" + exception.getMessage());
                GuiAlert.error("Cleanup thất bại", exception);
            } else {
                resultArea.setText("Lỗi cleanup không xác định.");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void cleanupAllUploadedBatches(Button button) {
        List<UploadedMarkedBatch> batches = new ArrayList<>(uploadedItems);

        if (batches.isEmpty()) {
            GuiAlert.warning(
                    "Không có batch cần dọn",
                    "Hiện không có batch nào ở trạng thái UPLOADED_MARKED."
            );
            return;
        }

        int totalVideoCount = 0;

        for (UploadedMarkedBatch batch : batches) {
            totalVideoCount = totalVideoCount + batch.getUploadedMarkedVideoCount();
        }

        boolean confirmed = confirm(
                "Xác nhận xóa toàn bộ?",
                "Thao tác này sẽ xóa toàn bộ file raw/edited của tất cả batch đã upload chờ dọn.\n\n"
                        + "Số batch sẽ dọn: "
                        + batches.size()
                        + "\nTổng video đã mark uploaded: "
                        + totalVideoCount
                        + "\n\nSau khi dọn:"
                        + "\n- File raw/edited của các batch này sẽ bị xóa."
                        + "\n- video_files trong DB sẽ bị xóa."
                        + "\n- videos vẫn giữ video ID để chống tải trùng."
        );

        if (!confirmed) {
            return;
        }

        button.setDisable(true);
        statusLabel.setText("Đang cleanup toàn bộ batch đã upload...");
        resultArea.setText("Đang xóa toàn bộ batch đã upload chờ dọn...\n"
                + "Số batch: "
                + batches.size()
                + "\n\nKhông tắt tool trong lúc đang xử lý.");

        Task<List<UploadCleanupResult>> task = new Task<>() {
            @Override
            protected List<UploadCleanupResult> call() {
                List<UploadCleanupResult> results = new ArrayList<>();

                for (UploadedMarkedBatch batch : batches) {
                    UploadCleanupResult result =
                            uploadCleanupService.cleanupUploadedBatch(batch);

                    results.add(result);
                }

                return results;
            }
        };

        task.setOnSucceeded(event -> {
            List<UploadCleanupResult> results = task.getValue();

            button.setDisable(false);
            refreshAll();

            statusLabel.setText("Cleanup toàn bộ hoàn tất.");
            resultArea.setText(buildCleanupAllResultText(results));

            int cleanedCount = 0;
            int failedCount = 0;
            int rawDeletedFileCount = 0;
            int editedDeletedFileCount = 0;

            for (UploadCleanupResult result : results) {
                if ("CLEANED".equalsIgnoreCase(result.getStatus())) {
                    cleanedCount++;
                } else {
                    failedCount++;
                }

                rawDeletedFileCount = rawDeletedFileCount + result.getRawDeletedFileCount();
                editedDeletedFileCount = editedDeletedFileCount + result.getEditedDeletedFileCount();
            }

            GuiAlert.info(
                    "Cleanup toàn bộ hoàn tất",
                    "Batch đã dọn thành công: " + cleanedCount
                            + "\nBatch lỗi: " + failedCount
                            + "\nRaw files đã xóa: " + rawDeletedFileCount
                            + "\nEdited files đã xóa: " + editedDeletedFileCount
            );
        });

        task.setOnFailed(event -> {
            button.setDisable(false);

            Throwable throwable = task.getException();
            statusLabel.setText("Cleanup toàn bộ thất bại.");

            if (throwable instanceof Exception exception) {
                resultArea.setText("Lỗi cleanup toàn bộ:\n" + exception.getMessage());
                GuiAlert.error("Cleanup toàn bộ thất bại", exception);
            } else {
                resultArea.setText("Lỗi cleanup toàn bộ không xác định.");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String buildMarkResultText(ManualUploadMarkResult result) {
        return "KẾT QUẢ MARK UPLOADED"
                + "\n======================"
                + "\nBatch: " + emptyToDash(result.getBatchCode())
                + "\nVideo batch ID: " + result.getVideoBatchId()
                + "\nSố video đã đánh dấu: " + result.getMarkedVideoCount()
                + "\nStatus mới: " + emptyToDash(result.getNewBatchStatus())
                + "\nThông báo: " + emptyToDash(result.getMessage());
    }

    private String buildCleanupResultText(UploadCleanupResult result) {
        return "KẾT QUẢ CLEANUP SAU UPLOAD"
                + "\n==========================="
                + "\nBatch: " + emptyToDash(result.getBatchCode())
                + "\nVideo batch ID: " + result.getVideoBatchId()
                + "\nTrạng thái: " + emptyToDash(result.getStatus())
                + "\nRaw files đã xóa: " + result.getRawDeletedFileCount()
                + "\nEdited files đã xóa: " + result.getEditedDeletedFileCount()
                + "\nRaw folder đã xóa: " + (result.isRawFolderDeleted() ? "Có" : "Không")
                + "\nEdited folder đã xóa: " + (result.isEditedFolderDeleted() ? "Có" : "Không")
                + "\nvideo_files đã xóa trong DB: " + result.getDatabaseVideoFilesDeletedCount()
                + "\nvideos đã đổi sang UPLOADED_DELETED: " + result.getDatabaseVideosUpdatedCount()
                + "\nThông báo: " + emptyToDash(result.getMessage());
    }

    private String buildCleanupAllResultText(List<UploadCleanupResult> results) {
        StringBuilder builder = new StringBuilder();

        int cleanedCount = 0;
        int failedCount = 0;
        int rawDeletedFileCount = 0;
        int editedDeletedFileCount = 0;
        int databaseVideoFilesDeletedCount = 0;
        int databaseVideosUpdatedCount = 0;

        for (UploadCleanupResult result : results) {
            if ("CLEANED".equalsIgnoreCase(result.getStatus())) {
                cleanedCount++;
            } else {
                failedCount++;
            }

            rawDeletedFileCount = rawDeletedFileCount + result.getRawDeletedFileCount();
            editedDeletedFileCount = editedDeletedFileCount + result.getEditedDeletedFileCount();
            databaseVideoFilesDeletedCount = databaseVideoFilesDeletedCount + result.getDatabaseVideoFilesDeletedCount();
            databaseVideosUpdatedCount = databaseVideosUpdatedCount + result.getDatabaseVideosUpdatedCount();
        }

        builder.append("KẾT QUẢ CLEANUP TOÀN BỘ")
                .append("\n========================")
                .append("\nTổng batch xử lý: ")
                .append(results.size())
                .append("\nBatch thành công: ")
                .append(cleanedCount)
                .append("\nBatch lỗi: ")
                .append(failedCount)
                .append("\nRaw files đã xóa: ")
                .append(rawDeletedFileCount)
                .append("\nEdited files đã xóa: ")
                .append(editedDeletedFileCount)
                .append("\nvideo_files đã xóa trong DB: ")
                .append(databaseVideoFilesDeletedCount)
                .append("\nvideos đã đổi sang UPLOADED_DELETED: ")
                .append(databaseVideosUpdatedCount)
                .append("\n");

        for (UploadCleanupResult result : results) {
            builder.append("\n------------------------------------")
                    .append("\nBatch: ")
                    .append(emptyToDash(result.getBatchCode()))
                    .append("\nVideo batch ID: ")
                    .append(result.getVideoBatchId())
                    .append("\nTrạng thái: ")
                    .append(emptyToDash(result.getStatus()))
                    .append("\nRaw files đã xóa: ")
                    .append(result.getRawDeletedFileCount())
                    .append("\nEdited files đã xóa: ")
                    .append(result.getEditedDeletedFileCount())
                    .append("\nThông báo: ")
                    .append(emptyToDash(result.getMessage()))
                    .append("\n");
        }

        return builder.toString();
    }

    private void openExistingFolder(Path folder) {
        try {
            if (folder == null) {
                GuiAlert.warning("Không có folder", "Đường dẫn folder đang bị trống.");
                return;
            }

            if (!Files.exists(folder)) {
                GuiAlert.warning(
                        "Folder không tồn tại",
                        "Không mở được folder:\n" + folder.toAbsolutePath()
                );
                return;
            }

            if (!Files.isDirectory(folder)) {
                GuiAlert.warning(
                        "Không phải folder",
                        "Đường dẫn này không phải folder:\n" + folder.toAbsolutePath()
                );
                return;
            }

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

    private void openOrCreateFolder(Path folder) {
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

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .isPresent();
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

    private String emptyToDash(String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }

        return text;
    }
}