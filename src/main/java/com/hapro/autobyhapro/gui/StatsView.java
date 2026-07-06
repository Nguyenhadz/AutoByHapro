package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.CsvExportResult;
import com.hapro.autobyhapro.entity.PageSimpleStats;
import com.hapro.autobyhapro.repository.SimpleStatsRepository;
import com.hapro.autobyhapro.service.CsvExportService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class StatsView {

    private final SimpleStatsRepository simpleStatsRepository = new SimpleStatsRepository();
    private final CsvExportService csvExportService = new CsvExportService();

    private final ScrollPane rootScrollPane = new ScrollPane();
    private final VBox root = new VBox(16);
    private final TableView<PageSimpleStats> tableView = new TableView<>();

    private final Label totalDownloadedLabel = summaryValueLabel("0");
    private final Label totalReadyUploadLabel = summaryValueLabel("0");
    private final Label totalUploadedLabel = summaryValueLabel("0");
    private final Label totalCleanedLabel = summaryValueLabel("0");

    public StatsView() {
        buildLayout();
        refreshStats();
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

        Label titleLabel = new Label("Thống kê");
        titleLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(
                "Xem thống kê tổng quan theo fanpage và xuất CSV để mở bằng WPS/Excel."
        );
        descriptionLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #4b5563;
                """);

        GridPane summaryGrid = buildSummaryGrid();
        VBox tableCard = buildTableCard();

        root.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                summaryGrid,
                tableCard
        );

        VBox.setVgrow(tableCard, Priority.ALWAYS);
    }

    private GridPane buildSummaryGrid() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(14);
        gridPane.setVgap(14);

        gridPane.add(summaryCard("Tổng video ID đã lưu", totalDownloadedLabel), 0, 0);
        gridPane.add(summaryCard("Đang chờ upload", totalReadyUploadLabel), 1, 0);
        gridPane.add(summaryCard("Đã upload", totalUploadedLabel), 2, 0);
        gridPane.add(summaryCard("Đã dọn file", totalCleanedLabel), 3, 0);

        return gridPane;
    }

    private VBox summaryCard(String title, Label valueLabel) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setPrefWidth(220);
        card.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 12;
                -fx-border-color: #e5e7eb;
                -fx-border-radius: 12;
                """);

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("""
                -fx-font-size: 13px;
                -fx-text-fill: #6b7280;
                """);

        card.getChildren().addAll(titleLabel, valueLabel);

        return card;
    }

    private VBox buildTableCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18));
        card.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 12;
                -fx-border-color: #e5e7eb;
                -fx-border-radius: 12;
                """);

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = new Label("Thống kê theo fanpage");
        cardTitle.setStyle("""
                -fx-font-size: 18px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = secondaryButton("Refresh");
        refreshButton.setOnAction(event -> refreshStats());

        Button exportPageStatsButton = primaryButton("Xuất thống kê CSV");
        exportPageStatsButton.setOnAction(event -> exportPageStatsCsv());

        Button exportVideosButton = primaryButton("Xuất video ID CSV");
        exportVideosButton.setOnAction(event -> exportVideosCsv());

        Button openExportsButton = secondaryButton("Mở exports");
        openExportsButton.setOnAction(event -> openExportsFolder());

        headerBox.getChildren().addAll(
                cardTitle,
                spacer,
                refreshButton,
                exportPageStatsButton,
                exportVideosButton,
                openExportsButton
        );

        Label noteLabel = new Label(
                "Tổng video ID đã lưu là dữ liệu dùng để chống tải trùng. Video đã dọn file vẫn còn ID trong DB."
        );
        noteLabel.setWrapText(true);
        noteLabel.setStyle("""
                -fx-font-size: 13px;
                -fx-text-fill: #6b7280;
                """);

        buildTableColumns();

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.setPlaceholder(new Label("Chưa có dữ liệu thống kê."));
        tableView.setPrefHeight(430);

        Button detailButton = secondaryButton("Xem chi tiết fanpage đã chọn");
        detailButton.setOnAction(event -> showSelectedPageDetail());

        HBox bottomBox = new HBox(10);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.getChildren().add(detailButton);

        card.getChildren().addAll(
                headerBox,
                noteLabel,
                tableView,
                bottomBox
        );

        VBox.setVgrow(tableView, Priority.ALWAYS);

        return card;
    }

    private void buildTableColumns() {
        TableColumn<PageSimpleStats, String> pageCodeColumn = new TableColumn<>("Page");
        pageCodeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getPageCode()))
        );

        TableColumn<PageSimpleStats, String> pageNameColumn = new TableColumn<>("Tên page");
        pageNameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getPageName()))
        );
        pageNameColumn.setPrefWidth(180);

        TableColumn<PageSimpleStats, String> sourceCodeColumn = new TableColumn<>("Source");
        sourceCodeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getSourceCode()))
        );

        TableColumn<PageSimpleStats, String> sourceNameColumn = new TableColumn<>("Tên source");
        sourceNameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getSourceName()))
        );
        sourceNameColumn.setPrefWidth(200);

        TableColumn<PageSimpleStats, Number> downloadedColumn = new TableColumn<>("Đã tải");
        downloadedColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getTotalDownloadedCount())
        );

        TableColumn<PageSimpleStats, Number> readyColumn = new TableColumn<>("Chờ up");
        readyColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getReadyUploadCount())
        );

        TableColumn<PageSimpleStats, Number> uploadedColumn = new TableColumn<>("Đã up");
        uploadedColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getUploadedTotalCount())
        );

        TableColumn<PageSimpleStats, Number> cleanedColumn = new TableColumn<>("Đã dọn");
        cleanedColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getUploadedDeletedCount())
        );

        TableColumn<PageSimpleStats, Number> workingDaysColumn = new TableColumn<>("Ngày làm");
        workingDaysColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getWorkingDays())
        );

        TableColumn<PageSimpleStats, String> startDateColumn = new TableColumn<>("Ngày bắt đầu");
        startDateColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(emptyToDash(data.getValue().getStartDate()))
        );
        startDateColumn.setPrefWidth(120);

        tableView.getColumns().setAll(
                pageCodeColumn,
                pageNameColumn,
                sourceCodeColumn,
                sourceNameColumn,
                downloadedColumn,
                readyColumn,
                uploadedColumn,
                cleanedColumn,
                workingDaysColumn,
                startDateColumn
        );
    }

    private void refreshStats() {
        try {
            List<PageSimpleStats> statsList = simpleStatsRepository.findPageSimpleStats();
            tableView.setItems(FXCollections.observableArrayList(statsList));
            updateSummary(statsList);

        } catch (Exception exception) {
            GuiAlert.error("Không thể tải thống kê", exception);
        }
    }

    private void updateSummary(List<PageSimpleStats> statsList) {
        int totalDownloaded = 0;
        int totalReadyUpload = 0;
        int totalUploaded = 0;
        int totalCleaned = 0;

        for (PageSimpleStats stats : statsList) {
            totalDownloaded = totalDownloaded + stats.getTotalDownloadedCount();
            totalReadyUpload = totalReadyUpload + stats.getReadyUploadCount();
            totalUploaded = totalUploaded + stats.getUploadedTotalCount();
            totalCleaned = totalCleaned + stats.getUploadedDeletedCount();
        }

        totalDownloadedLabel.setText(String.valueOf(totalDownloaded));
        totalReadyUploadLabel.setText(String.valueOf(totalReadyUpload));
        totalUploadedLabel.setText(String.valueOf(totalUploaded));
        totalCleanedLabel.setText(String.valueOf(totalCleaned));
    }

    private void showSelectedPageDetail() {
        PageSimpleStats stats = tableView.getSelectionModel().getSelectedItem();

        if (stats == null) {
            GuiAlert.warning("Chưa chọn fanpage", "M chọn một dòng fanpage trong bảng trước.");
            return;
        }

        String message = "Page: " + stats.getPageCode() + " - " + stats.getPageName()
                + "\nSource active: " + emptyToDash(stats.getSourceCode()) + " - " + emptyToDash(stats.getSourceName())
                + "\nLoại source: " + emptyToDash(stats.getSourceType())
                + "\nNgày bắt đầu có dữ liệu: " + emptyToDash(stats.getStartDate())
                + "\nSố ngày làm việc: " + stats.getWorkingDays()
                + "\n\nTổng video ID đã lưu: " + stats.getTotalDownloadedCount()
                + "\nVideo đang chờ upload: " + stats.getReadyUploadCount()
                + "\nVideo đã đánh dấu upload, chưa dọn: " + stats.getUploadedMarkedCount()
                + "\nVideo đã upload và đã dọn file: " + stats.getUploadedDeletedCount()
                + "\nTổng video đã upload: " + stats.getUploadedTotalCount();

        GuiAlert.info("Chi tiết fanpage", message);
    }

    private void exportPageStatsCsv() {
        try {
            CsvExportResult result = csvExportService.exportPageStatsCsv();

            GuiAlert.info(
                    "Xuất CSV thành công",
                    "Số dòng: " + result.getRowCount()
                            + "\nFile: " + result.getFilePath()
            );

        } catch (Exception exception) {
            GuiAlert.error("Không thể xuất thống kê CSV", exception);
        }
    }

    private void exportVideosCsv() {
        try {
            CsvExportResult result = csvExportService.exportVideosCsv();

            GuiAlert.info(
                    "Xuất CSV thành công",
                    "Số dòng: " + result.getRowCount()
                            + "\nFile: " + result.getFilePath()
            );

        } catch (Exception exception) {
            GuiAlert.error("Không thể xuất video ID CSV", exception);
        }
    }

    private void openExportsFolder() {
        try {
            Files.createDirectories(AppPaths.EXPORTS_DIR);

            new ProcessBuilder(
                    "explorer.exe",
                    AppPaths.EXPORTS_DIR.toAbsolutePath().toString()
            ).start();

        } catch (IOException exception) {
            GuiAlert.error("Không thể mở folder exports", exception);
        }
    }

    private Label summaryValueLabel(String text) {
        Label label = new Label(text);
        label.setStyle("""
                -fx-font-size: 26px;
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

    private String nullToEmpty(String text) {
        if (text == null) {
            return "";
        }

        return text;
    }

    private String emptyToDash(String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }

        return text;
    }
}
