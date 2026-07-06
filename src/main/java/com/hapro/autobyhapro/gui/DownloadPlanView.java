package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.entity.AutoDownloadJobResult;
import com.hapro.autobyhapro.entity.AutoDownloadPageResult;
import com.hapro.autobyhapro.entity.DownloadPlanGuiRow;
import com.hapro.autobyhapro.entity.DownloadTarget;
import com.hapro.autobyhapro.entity.VideoBatchFolder;
import com.hapro.autobyhapro.entity.VideoCandidate;
import com.hapro.autobyhapro.entity.VideoDownloadItemResult;
import com.hapro.autobyhapro.service.AutoDownloadJobService;
import com.hapro.autobyhapro.service.DownloadPlanGuiService;
import com.hapro.autobyhapro.config.AppPaths;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.TextField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloadPlanView {

    private final DownloadPlanGuiService downloadPlanGuiService =
            new DownloadPlanGuiService();

    private final AutoDownloadJobService autoDownloadJobService =
            new AutoDownloadJobService();

    private final ScrollPane rootScrollPane = new ScrollPane();
    private final VBox root = new VBox(14);

    private final TableView<DownloadPlanGuiRow> tableView = new TableView<>();
    private final TextArea resultArea = new TextArea();
    private final Label statusLabel = new Label("Sẵn sàng.");

    private final TextField threadCountField = new TextField("1");

    private final CheckBox selectAllHeaderCheckBox = new CheckBox();
    private final ObservableList<DownloadPlanGuiRow> tableItems =
            FXCollections.observableArrayList();

    public DownloadPlanView() {
        buildLayout();
        loadRows();
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

        Label titleLabel = new Label("Kế hoạch download");
        titleLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(
                "Chọn fanpage cần tải video, nhập số lượng cần tải và chạy download."
        );
        descriptionLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #4b5563;
                """);

        VBox tableCard = buildTableCard();
        VBox resultCard = buildResultCard();

        root.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                tableCard,
                resultCard
        );
    }

    private VBox buildTableCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("Chọn fanpage cần tải");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        threadCountField.setPromptText("Mặc định 1");
        threadCountField.setMinWidth(90);
        threadCountField.setPrefWidth(110);
        threadCountField.setMaxWidth(120);
        threadCountField.setStyle("""
        -fx-font-size: 13px;
        """);

        Button refreshButton = secondaryButton("Refresh");
        refreshButton.setOnAction(event -> loadRows());

        Button buildPlanButton = primaryButton("Tạo kế hoạch");
        buildPlanButton.setOnAction(event -> buildPlan());

        Button startDownloadButton = dangerButton("Bắt đầu download");
        startDownloadButton.setOnAction(event -> startDownload(startDownloadButton));

        Button openRawFolderButton = secondaryButton("Mở folder raw");
        openRawFolderButton.setOnAction(event -> openRawFolder());

        Label threadCountLabel = new Label("Số luồng tải:");
        threadCountLabel.setStyle("""
        -fx-font-size: 13px;
        -fx-text-fill: #374151;
        """);

        headerBox.getChildren().addAll(
                cardTitle,
                spacer,
                threadCountLabel,
                threadCountField,
                refreshButton,
                buildPlanButton,
                startDownloadButton,
                openRawFolderButton
        );

        Label noteLabel = note(
                "Số luồng tải mặc định là 1. Có thể nhập tùy ý, ví dụ 1, 2, 3, 5. Nên test ít luồng trước để tránh mạng hoặc SQLite bị quá tải."
        );

        buildTableColumns();

        tableView.setItems(tableItems);
        tableView.setEditable(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.setPlaceholder(new Label("Chưa có fanpage active."));
        tableView.setPrefHeight(390);

        card.getChildren().addAll(
                headerBox,
                noteLabel,
                tableView
        );

        return card;
    }

    private VBox buildResultCard() {
        VBox card = card();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = cardTitle("Kết quả download");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearButton = secondaryButton("Xóa kết quả");
        clearButton.setOnAction(event -> {
            resultArea.clear();
            statusLabel.setText("Đã xóa kết quả.");
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
        resultArea.setPrefHeight(260);
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
        TableColumn<DownloadPlanGuiRow, String> sttColumn = new TableColumn<>("STT");
        sttColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(""));
        sttColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    return;
                }

                setText(String.valueOf(getIndex() + 1));
                setAlignment(Pos.CENTER);
            }
        });
        sttColumn.setSortable(false);
        sttColumn.setPrefWidth(55);
        sttColumn.setMaxWidth(65);

        TableColumn<DownloadPlanGuiRow, Boolean> selectedColumn = new TableColumn<>();
        selectedColumn.setCellValueFactory(data -> data.getValue().selectedProperty());
        selectedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectedColumn));
        selectedColumn.setEditable(true);
        selectedColumn.setSortable(false);
        selectedColumn.setStyle("-fx-alignment: CENTER;");
        selectedColumn.setPrefWidth(70);
        selectedColumn.setMaxWidth(80);

        selectAllHeaderCheckBox.setAllowIndeterminate(false);
        selectAllHeaderCheckBox.setOnAction(event -> {
            boolean selected = selectAllHeaderCheckBox.isSelected();
            selectAllHeaderCheckBox.setIndeterminate(false);
            setAllSelected(selected);
        });

        selectedColumn.setGraphic(selectAllHeaderCheckBox);

        TableColumn<DownloadPlanGuiRow, String> pageColumn = new TableColumn<>("Fanpage");
        pageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        emptyToDash(data.getValue().getPageCode())
                                + " - "
                                + emptyToDash(data.getValue().getPageName())
                )
        );
        pageColumn.setPrefWidth(190);

        TableColumn<DownloadPlanGuiRow, String> sourceColumn = new TableColumn<>("Source active");
        sourceColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        emptyToDash(data.getValue().getSourceCode())
                                + " - "
                                + emptyToDash(data.getValue().getSourceName())
                )
        );
        sourceColumn.setPrefWidth(210);

        TableColumn<DownloadPlanGuiRow, String> typeColumn = new TableColumn<>("Loại");
        typeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getSourceType()))
        );

        TableColumn<DownloadPlanGuiRow, Number> countColumn = new TableColumn<>("Số video tải");
        countColumn.setCellValueFactory(data -> data.getValue().downloadCountProperty());
        countColumn.setCellFactory(column -> new TableCell<>() {
            private final Spinner<Integer> spinner = new Spinner<>();

            {
                spinner.setEditable(true);
                spinner.setMaxWidth(95);
            }

            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                DownloadPlanGuiRow row = getTableView().getItems().get(getIndex());

                SpinnerValueFactory<Integer> valueFactory =
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(
                                1,
                                999,
                                row.getDownloadCount()
                        );

                spinner.setValueFactory(valueFactory);

                valueFactory.valueProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        row.setDownloadCount(newValue);
                    }
                });

                setGraphic(spinner);
                setAlignment(Pos.CENTER);
            }
        });
        countColumn.setPrefWidth(120);

        TableColumn<DownloadPlanGuiRow, String> defaultColumn = new TableColumn<>("Mặc định");
        defaultColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.valueOf(data.getValue().getDefaultVideoCount()))
        );

        TableColumn<DownloadPlanGuiRow, String> urlColumn = new TableColumn<>("Link source");
        urlColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getSourceUrl()))
        );
        urlColumn.setPrefWidth(260);

        tableView.getColumns().setAll(
                sttColumn,
                pageColumn,
                sourceColumn,
                typeColumn,
                countColumn,
                defaultColumn,
                urlColumn,
                selectedColumn
        );
    }

    private void loadRows() {
        try {
            List<DownloadPlanGuiRow> rows = downloadPlanGuiService.loadRows();

            tableItems.setAll(rows);

            for (DownloadPlanGuiRow row : rows) {
                row.selectedProperty().addListener((observable, oldValue, newValue) ->
                        updateSelectAllHeaderState()
                );
            }

            updateSelectAllHeaderState();

            statusLabel.setText("Đã load " + rows.size() + " fanpage active.");
            resultArea.setText("Đã load danh sách fanpage active.\nSố fanpage: " + rows.size());

        } catch (Exception exception) {
            statusLabel.setText("Load kế hoạch thất bại.");
            resultArea.setText("Lỗi load kế hoạch:\n" + exception.getMessage());
            GuiAlert.error("Không thể load kế hoạch download", exception);
        }
    }

    private void buildPlan() {
        List<DownloadPlanGuiRow> selectedRows = getSelectedRows();

        if (!validateSelectedRows(selectedRows)) {
            return;
        }

        int threadCount;

        try {
            threadCount = readThreadCount();
        } catch (IllegalArgumentException exception) {
            GuiAlert.warning("Số luồng không hợp lệ", exception.getMessage());
            return;
        }

        String planText = downloadPlanGuiService.buildPlanText(
                selectedRows,
                threadCount
        );

        statusLabel.setText("Đã tạo kế hoạch download.");
        resultArea.setText(planText);
    }

    private void startDownload(Button startDownloadButton) {
        List<DownloadPlanGuiRow> selectedRows = getSelectedRows();

        if (!validateSelectedRows(selectedRows)) {
            return;
        }

        int threadCount;

        try {
            threadCount = readThreadCount();
        } catch (IllegalArgumentException exception) {
            GuiAlert.warning("Số luồng không hợp lệ", exception.getMessage());
            return;
        }

        List<DownloadTarget> selectedTargets = new ArrayList<>();
        Map<Long, Integer> requestedCountByFanpageId = new LinkedHashMap<>();

        for (DownloadPlanGuiRow row : selectedRows) {
            DownloadTarget target = toDownloadTarget(row);

            selectedTargets.add(target);
            requestedCountByFanpageId.put(row.getFanpageId(), row.getDownloadCount());
        }

        String beforeRunText = downloadPlanGuiService.buildPlanText(
                selectedRows,
                threadCount
        );

        statusLabel.setText("Đang download...");
        resultArea.setText(beforeRunText
                + "\n\nĐANG CHẠY DOWNLOAD..."
                + "\nKhông tắt tool trong lúc đang tải.");

        startDownloadButton.setDisable(true);

        Task<AutoDownloadJobResult> task = new Task<>() {
            @Override
            protected AutoDownloadJobResult call() {
                return autoDownloadJobService.runJob(
                        selectedTargets,
                        requestedCountByFanpageId,
                        threadCount
                );
            }
        };

        task.setOnSucceeded(event -> {
            AutoDownloadJobResult result = task.getValue();

            statusLabel.setText("Download hoàn tất.");
            resultArea.setText(buildJobResultText(result));

            startDownloadButton.setDisable(false);

            GuiAlert.info(
                    "Download hoàn tất",
                    "Tổng video tải thành công: " + result.getTotalDownloadedCount()
                            + "\nTải thất bại: " + result.getTotalFailedCount()
                            + "\nFolder batch đã tạo: " + result.getTotalFolderCount()
            );
        });

        task.setOnFailed(event -> {
            Throwable throwable = task.getException();

            statusLabel.setText("Download thất bại.");
            startDownloadButton.setDisable(false);

            if (throwable instanceof Exception exception) {
                resultArea.setText("Lỗi download:\n" + exception.getMessage());
                GuiAlert.error("Download thất bại", exception);
            } else {
                resultArea.setText("Lỗi download không xác định.");
                GuiAlert.warning("Download thất bại", "Lỗi không xác định.");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private List<DownloadPlanGuiRow> getSelectedRows() {
        return tableItems
                .stream()
                .filter(DownloadPlanGuiRow::isSelected)
                .toList();
    }

    private boolean validateSelectedRows(List<DownloadPlanGuiRow> selectedRows) {
        if (selectedRows == null || selectedRows.isEmpty()) {
            GuiAlert.warning("Chưa chọn fanpage", "M cần tick chọn ít nhất 1 fanpage.");
            return false;
        }

        for (DownloadPlanGuiRow row : selectedRows) {
            if (row.getDownloadCount() <= 0) {
                GuiAlert.warning(
                        "Số lượng không hợp lệ",
                        row.getPageCode() + " phải có số video tải lớn hơn 0."
                );
                return false;
            }

            if (row.getSourceId() == null
                    || row.getSourceUrl() == null
                    || row.getSourceUrl().isBlank()) {
                GuiAlert.warning(
                        "Fanpage chưa có source active",
                        row.getPageCode()
                                + " - "
                                + row.getPageName()
                                + " chưa có source active, không thể download."
                );
                return false;
            }
        }

        return true;
    }

    private DownloadTarget toDownloadTarget(DownloadPlanGuiRow row) {
        return new DownloadTarget(
                row.getFanpageId(),
                row.getPageCode(),
                row.getPageName(),
                row.getDefaultVideoCount(),
                row.getSourceId(),
                row.getSourceCode(),
                row.getSourceName(),
                row.getSourceType(),
                row.getSourceUrl()
        );
    }

    private String buildJobResultText(AutoDownloadJobResult result) {
        StringBuilder builder = new StringBuilder();

        builder.append("KẾT QUẢ DOWNLOAD")
                .append(System.lineSeparator());
        builder.append("================")
                .append(System.lineSeparator());

        builder.append("Số luồng đã chọn: ")
                .append(result.getThreadCount())
                .append(System.lineSeparator());

        builder.append("Tổng page xử lý: ")
                .append(result.getTotalPages())
                .append(System.lineSeparator());

        builder.append("Tổng video yêu cầu: ")
                .append(result.getTotalRequestedCount())
                .append(System.lineSeparator());

        builder.append("Tổng video mới tìm được: ")
                .append(result.getTotalFoundNewCount())
                .append(System.lineSeparator());

        builder.append("Tổng video tải thành công: ")
                .append(result.getTotalDownloadedCount())
                .append(System.lineSeparator());

        builder.append("Tổng video tải thất bại: ")
                .append(result.getTotalFailedCount())
                .append(System.lineSeparator());

        builder.append("Tổng folder batch đã tạo: ")
                .append(result.getTotalFolderCount())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        for (AutoDownloadPageResult pageResult : result.getPageResults()) {
            builder.append(buildPageResultText(pageResult))
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    private String buildPageResultText(AutoDownloadPageResult result) {
        StringBuilder builder = new StringBuilder();

        builder.append("------------------------------------")
                .append(System.lineSeparator());

        builder.append(result.getTarget().getFanpageCode())
                .append(" - ")
                .append(result.getTarget().getFanpageName())
                .append(System.lineSeparator());

        builder.append("Source: ")
                .append(emptyToDash(result.getTarget().getSourceCode()))
                .append(" - ")
                .append(emptyToDash(result.getTarget().getSourceName()))
                .append(System.lineSeparator());

        builder.append("Trạng thái: ")
                .append(emptyToDash(result.getStatus()))
                .append(System.lineSeparator());

        builder.append("Yêu cầu: ")
                .append(result.getRequestedCount())
                .append(System.lineSeparator());

        builder.append("Đã quét từ source: ")
                .append(result.getScannedCount())
                .append(System.lineSeparator());

        builder.append("Đã có trong DB, bỏ qua: ")
                .append(result.getSkippedExistingCount())
                .append(System.lineSeparator());

        builder.append("Video mới tìm được: ")
                .append(result.getFoundNewCount())
                .append(System.lineSeparator());

        builder.append("Tải thành công: ")
                .append(result.getDownloadedCount())
                .append(System.lineSeparator());

        builder.append("Tải thất bại: ")
                .append(result.getFailedCount())
                .append(System.lineSeparator());

        builder.append("Thông báo: ")
                .append(emptyToDash(result.getMessage()))
                .append(System.lineSeparator());

        if (result.getDownloadBatchId() != null) {
            builder.append("Download batch ID: ")
                    .append(result.getDownloadBatchId())
                    .append(System.lineSeparator());
        }

        if (!result.getVideoBatchFolders().isEmpty()) {
            builder.append(System.lineSeparator());
            builder.append("Folder batch:")
                    .append(System.lineSeparator());

            for (VideoBatchFolder folder : result.getVideoBatchFolders()) {
                builder.append("- ")
                        .append(folder.getBatchCode())
                        .append(" | ")
                        .append(folder.getVideoCount())
                        .append(" video")
                        .append(System.lineSeparator());

                builder.append("  Raw: ")
                        .append(folder.getRawFolderPath().toAbsolutePath())
                        .append(System.lineSeparator());

                builder.append("  Edited: ")
                        .append(folder.getEditedFolderPath().toAbsolutePath())
                        .append(System.lineSeparator());
            }
        }

        if (!result.getDownloadItemResults().isEmpty()) {
            builder.append(System.lineSeparator());
            builder.append("Chi tiết tải video:")
                    .append(System.lineSeparator());

            for (VideoDownloadItemResult item : result.getDownloadItemResults()) {
                VideoCandidate video = item.getVideo();

                builder.append(item.isSuccess() ? "OK" : "FAIL")
                        .append(" | ")
                        .append(emptyToDash(item.getBatchCode()))
                        .append(" | ")
                        .append(shorten(video.getVideoId(), 18))
                        .append(" | ")
                        .append(shorten(video.getTitle(), 55))
                        .append(System.lineSeparator());

                if (item.isSuccess()) {
                    builder.append("     File: ")
                            .append(emptyToDash(item.getFilePath()))
                            .append(System.lineSeparator());
                } else {
                    builder.append("     Lỗi: ")
                            .append(emptyToDash(item.getMessage()))
                            .append(System.lineSeparator());
                }
            }
        }

        return builder.toString();
    }

    private int readThreadCount() {
        String text = threadCountField.getText();

        if (text == null || text.isBlank()) {
            threadCountField.setText("1");
            return 1;
        }

        try {
            int threadCount = Integer.parseInt(text.trim());

            if (threadCount < 1) {
                throw new NumberFormatException("Thread count must be positive.");
            }

            return threadCount;

        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Số luồng tải phải là số nguyên dương.\nVí dụ: 1, 2, 3, 5, 10."
            );
        }
    }
    private void setAllSelected(boolean selected) {
        for (DownloadPlanGuiRow row : tableItems) {
            row.setSelected(selected);
        }

        tableView.refresh();
        updateSelectAllHeaderState();

        if (selected) {
            statusLabel.setText("Đã chọn tất cả fanpage.");
        } else {
            statusLabel.setText("Đã bỏ chọn tất cả fanpage.");
        }
    }

    private void updateSelectAllHeaderState() {
        if (tableItems.isEmpty()) {
            selectAllHeaderCheckBox.setSelected(false);
            selectAllHeaderCheckBox.setIndeterminate(false);
            return;
        }

        long selectedCount = tableItems
                .stream()
                .filter(DownloadPlanGuiRow::isSelected)
                .count();

        if (selectedCount == 0) {
            selectAllHeaderCheckBox.setSelected(false);
            selectAllHeaderCheckBox.setIndeterminate(false);
            return;
        }

        if (selectedCount == tableItems.size()) {
            selectAllHeaderCheckBox.setSelected(true);
            selectAllHeaderCheckBox.setIndeterminate(false);
            return;
        }

        selectAllHeaderCheckBox.setSelected(false);
        selectAllHeaderCheckBox.setIndeterminate(true);
    }

    private void openRawFolder() {
        openFolder(
                AppPaths.RAW_DIR,
                "Đã mở folder raw:\n" + AppPaths.RAW_DIR.toAbsolutePath()
        );
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

            statusLabel.setText("Đã mở folder.");
            resultArea.setText(resultMessage);

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

    private String shorten(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 3) + "...";
    }
}