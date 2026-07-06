package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.entity.SourceCheckResult;
import com.hapro.autobyhapro.service.SourceHealthCheckService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
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

import java.util.List;

public class CheckSourceView {

    private final SourceHealthCheckService sourceHealthCheckService =
            new SourceHealthCheckService();

    private final ScrollPane rootScrollPane = new ScrollPane();
    private final VBox root = new VBox(14);

    private final TableView<SourceCheckResult> tableView = new TableView<>();
    private final TextArea resultArea = new TextArea();
    private final Label statusLabel = new Label("Sẵn sàng.");

    private final ObservableList<SourceCheckResult> tableItems =
            FXCollections.observableArrayList();

    public CheckSourceView() {
        buildLayout();
        loadSources();
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

        Label titleLabel = new Label("Kiểm tra source");
        titleLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(
                "Kiểm tra source active còn quét được video không và đếm số video chưa tải theo DB."
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

        Label cardTitle = cardTitle("Danh sách source active");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = secondaryButton("Refresh source");
        refreshButton.setOnAction(event -> loadSources());

        Button checkSelectedButton = primaryButton("Kiểm tra source đã chọn");
        checkSelectedButton.setOnAction(event -> checkSelectedSource());

        Button checkAllButton = primaryButton("Kiểm tra tất cả active");
        checkAllButton.setOnAction(event -> checkAllSources());

        headerBox.getChildren().addAll(
                cardTitle,
                spacer,
                refreshButton,
                checkSelectedButton,
                checkAllButton
        );

        Label noteLabel = note(
                "Nếu source có rất nhiều video, kiểm tra tất cả có thể chạy lâu. Tool dùng yt-dlp để quét danh sách video."
        );

        buildTableColumns();

        tableView.setItems(tableItems);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.setPlaceholder(new Label("Chưa có source active."));
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

        Label cardTitle = cardTitle("Kết quả kiểm tra");

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
        resultArea.setPrefHeight(220);
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
        TableColumn<SourceCheckResult, String> pageColumn = new TableColumn<>("Page");
        pageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        emptyToDash(data.getValue().getPageCode())
                                + " - "
                                + emptyToDash(data.getValue().getPageName())
                )
        );
        pageColumn.setPrefWidth(170);

        TableColumn<SourceCheckResult, String> sourceColumn = new TableColumn<>("Source");
        sourceColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getSourceCode()))
        );

        TableColumn<SourceCheckResult, String> sourceNameColumn = new TableColumn<>("Tên source");
        sourceNameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getSourceName()))
        );
        sourceNameColumn.setPrefWidth(180);

        TableColumn<SourceCheckResult, String> typeColumn = new TableColumn<>("Loại");
        typeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getSourceType()))
        );

        TableColumn<SourceCheckResult, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getStatus()))
        );

        TableColumn<SourceCheckResult, Number> totalColumn = new TableColumn<>("Tổng quét");
        totalColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getTotalFoundCount())
        );

        TableColumn<SourceCheckResult, Number> downloadedColumn = new TableColumn<>("Đã có");
        downloadedColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getAlreadyDownloadedCount())
        );

        TableColumn<SourceCheckResult, Number> newColumn = new TableColumn<>("Chưa tải");
        newColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getNotYetDownloadedCount())
        );

        TableColumn<SourceCheckResult, String> urlColumn = new TableColumn<>("Link");
        urlColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getSourceUrl()))
        );
        urlColumn.setPrefWidth(250);

        tableView.getColumns().setAll(
                pageColumn,
                sourceColumn,
                sourceNameColumn,
                typeColumn,
                statusColumn,
                totalColumn,
                downloadedColumn,
                newColumn,
                urlColumn
        );
    }

    private void loadSources() {
        try {
            List<SourceCheckResult> sources =
                    sourceHealthCheckService.findActiveSourcesForCheck();

            tableItems.setAll(sources);

            setStatus("Đã load " + sources.size() + " source active.");
            setResult("Đã load danh sách source active.\nSố source: " + sources.size());

        } catch (Exception exception) {
            setStatus("Load source thất bại.");
            setResult("Lỗi load source:\n" + exception.getMessage());
            GuiAlert.error("Không thể load source", exception);
        }
    }

    private void checkSelectedSource() {
        SourceCheckResult selectedSource =
                tableView.getSelectionModel().getSelectedItem();

        if (selectedSource == null) {
            GuiAlert.warning("Chưa chọn source", "M chọn một source trong bảng trước.");
            return;
        }

        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();

        runCheckOneSource(selectedSource, selectedIndex);
    }

    private void checkAllSources() {
        if (tableItems.isEmpty()) {
            GuiAlert.warning("Không có source", "Hiện chưa có source active để kiểm tra.");
            return;
        }

        setStatus("Đang kiểm tra tất cả source...");
        setResult("Đang kiểm tra tất cả source active...\nTổng source: " + tableItems.size());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                for (int index = 0; index < tableItems.size(); index++) {
                    SourceCheckResult source = tableItems.get(index);

                    int currentIndex = index;

                    Platform.runLater(() -> {
                        setStatus("Đang kiểm tra: " + source.getSourceCode() + " - " + source.getSourceName());
                        setResult("Đang kiểm tra source "
                                + (currentIndex + 1)
                                + "/"
                                + tableItems.size()
                                + "\n\n"
                                + source.getSourceCode()
                                + " - "
                                + source.getSourceName()
                                + "\n"
                                + source.getSourceUrl());
                    });

                    SourceCheckResult checkedResult =
                            sourceHealthCheckService.checkSource(source);

                    Platform.runLater(() -> {
                        tableItems.set(currentIndex, checkedResult);
                        tableView.getSelectionModel().select(currentIndex);
                        tableView.scrollTo(currentIndex);

                        setResult(buildResultText(checkedResult));
                    });
                }

                return null;
            }
        };

        task.setOnSucceeded(event -> {
            setStatus("Đã kiểm tra xong tất cả source.");
            setResult(buildAllSummaryText());
        });

        task.setOnFailed(event -> {
            Throwable throwable = task.getException();

            setStatus("Kiểm tra tất cả source thất bại.");

            if (throwable instanceof Exception exception) {
                setResult("Lỗi:\n" + exception.getMessage());
                GuiAlert.error("Kiểm tra source thất bại", exception);
            } else {
                setResult("Lỗi không xác định.");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void runCheckOneSource(SourceCheckResult source, int index) {
        setStatus("Đang kiểm tra: " + source.getSourceCode() + " - " + source.getSourceName());
        setResult("Đang kiểm tra source:\n"
                + source.getSourceCode()
                + " - "
                + source.getSourceName()
                + "\n"
                + source.getSourceUrl());

        Task<SourceCheckResult> task = new Task<>() {
            @Override
            protected SourceCheckResult call() {
                return sourceHealthCheckService.checkSource(source);
            }
        };

        task.setOnSucceeded(event -> {
            SourceCheckResult checkedResult = task.getValue();

            tableItems.set(index, checkedResult);
            tableView.getSelectionModel().select(index);
            tableView.scrollTo(index);

            setStatus("Hoàn thành.");
            setResult(buildResultText(checkedResult));
        });

        task.setOnFailed(event -> {
            Throwable throwable = task.getException();

            setStatus("Kiểm tra source thất bại.");

            if (throwable instanceof Exception exception) {
                setResult("Lỗi:\n" + exception.getMessage());
                GuiAlert.error("Kiểm tra source thất bại", exception);
            } else {
                setResult("Lỗi không xác định.");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String buildResultText(SourceCheckResult result) {
        return "SOURCE: " + emptyToDash(result.getSourceCode())
                + " - "
                + emptyToDash(result.getSourceName())
                + "\nPAGE: "
                + emptyToDash(result.getPageCode())
                + " - "
                + emptyToDash(result.getPageName())
                + "\nLOẠI: "
                + emptyToDash(result.getSourceType())
                + "\nLINK: "
                + emptyToDash(result.getSourceUrl())
                + "\n\nTRẠNG THÁI: "
                + emptyToDash(result.getStatus())
                + "\nTổng video quét được: "
                + result.getTotalFoundCount()
                + "\nĐã có trong DB: "
                + result.getAlreadyDownloadedCount()
                + "\nChưa tải: "
                + result.getNotYetDownloadedCount()
                + "\n\nGHI CHÚ:\n"
                + emptyToDash(result.getMessage());
    }

    private String buildAllSummaryText() {
        int okCount = 0;
        int warningCount = 0;
        int errorCount = 0;
        int totalFound = 0;
        int totalAlready = 0;
        int totalNew = 0;

        for (SourceCheckResult item : tableItems) {
            if ("OK".equalsIgnoreCase(item.getStatus())) {
                okCount++;
            } else if ("Cảnh báo".equalsIgnoreCase(item.getStatus())) {
                warningCount++;
            } else if ("Lỗi".equalsIgnoreCase(item.getStatus())
                    || "Quá lâu".equalsIgnoreCase(item.getStatus())) {
                errorCount++;
            }

            totalFound = totalFound + item.getTotalFoundCount();
            totalAlready = totalAlready + item.getAlreadyDownloadedCount();
            totalNew = totalNew + item.getNotYetDownloadedCount();
        }

        return "ĐÃ KIỂM TRA XONG TẤT CẢ SOURCE"
                + "\n\nTổng source: " + tableItems.size()
                + "\nOK: " + okCount
                + "\nCảnh báo: " + warningCount
                + "\nLỗi/quá lâu: " + errorCount
                + "\n\nTổng video quét được: " + totalFound
                + "\nĐã có trong DB: " + totalAlready
                + "\nChưa tải: " + totalNew;
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
