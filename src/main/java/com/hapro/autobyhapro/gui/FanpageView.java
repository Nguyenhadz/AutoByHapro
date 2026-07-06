package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.entity.Fanpage;
import com.hapro.autobyhapro.entity.FanpageDeleteResult;
import com.hapro.autobyhapro.repository.FanpageRepository;
import com.hapro.autobyhapro.service.FanpageDeleteService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import javafx.scene.control.TableCell;
import javafx.geometry.Pos;
import javafx.beans.property.ReadOnlyStringWrapper;

public class FanpageView {

    private final FanpageRepository fanpageRepository = new FanpageRepository();
    private final FanpageDeleteService fanpageDeleteService = new FanpageDeleteService();

    private final VBox root = new VBox(16);
    private final TableView<Fanpage> tableView = new TableView<>();

    private final TextField pageCodeField = new TextField();
    private final TextField pageNameField = new TextField();
    private final TextField pageUrlField = new TextField();
    private final TextField nicheField = new TextField();
    private final TextField defaultVideoCountField = new TextField();

    public FanpageView() {
        buildLayout();
        loadNextPageCode();
        refreshTable();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildLayout() {
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: transparent;");

        Label titleLabel = new Label("Quản lý fanpage");
        titleLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(
                "Thêm fanpage mới, xem danh sách fanpage và xóa toàn bộ fanpage khi không dùng nữa."
        );
        descriptionLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #4b5563;
                """);

        VBox formCard = buildFormCard();
        VBox tableCard = buildTableCard();

        root.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                formCard,
                tableCard
        );

        VBox.setVgrow(tableCard, Priority.ALWAYS);
    }

    private VBox buildFormCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18));
        card.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 12;
                -fx-border-color: #e5e7eb;
                -fx-border-radius: 12;
                """);

        Label cardTitle = new Label("Thêm fanpage mới");
        cardTitle.setStyle("""
                -fx-font-size: 18px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        pageCodeField.setEditable(false);
        pageCodeField.setPromptText("Tự động tạo, ví dụ P001");

        pageNameField.setPromptText("Ví dụ: Page Test 1");
        pageUrlField.setPromptText("Có thể bỏ trống");
        nicheField.setPromptText("Ví dụ: Funny, Motivation, Pet...");
        defaultVideoCountField.setPromptText("Mặc định 6");
        defaultVideoCountField.setText("6");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(14);
        gridPane.setVgap(12);

        ColumnConstraints labelColumn1 = new ColumnConstraints();
        labelColumn1.setMinWidth(120);

        ColumnConstraints fieldColumn1 = new ColumnConstraints();
        fieldColumn1.setHgrow(Priority.ALWAYS);

        ColumnConstraints labelColumn2 = new ColumnConstraints();
        labelColumn2.setMinWidth(120);

        ColumnConstraints fieldColumn2 = new ColumnConstraints();
        fieldColumn2.setHgrow(Priority.ALWAYS);

        gridPane.getColumnConstraints().addAll(
                labelColumn1,
                fieldColumn1,
                labelColumn2,
                fieldColumn2
        );

        gridPane.add(label("Mã page"), 0, 0);
        gridPane.add(pageCodeField, 1, 0);

        gridPane.add(label("Tên fanpage *"), 2, 0);
        gridPane.add(pageNameField, 3, 0);

        gridPane.add(label("Link fanpage"), 0, 1);
        gridPane.add(pageUrlField, 1, 1);

        gridPane.add(label("Niche/chủ đề"), 2, 1);
        gridPane.add(nicheField, 3, 1);

        gridPane.add(label("Số video mặc định"), 0, 2);
        gridPane.add(defaultVideoCountField, 1, 2);

        pageCodeField.setMaxWidth(Double.MAX_VALUE);
        pageNameField.setMaxWidth(Double.MAX_VALUE);
        pageUrlField.setMaxWidth(Double.MAX_VALUE);
        nicheField.setMaxWidth(Double.MAX_VALUE);
        defaultVideoCountField.setMaxWidth(Double.MAX_VALUE);

        Button saveButton = primaryButton("Thêm fanpage");
        saveButton.setOnAction(event -> saveFanpage());

        Button clearButton = secondaryButton("Làm mới form");
        clearButton.setOnAction(event -> clearForm());

        Button refreshButton = secondaryButton("Refresh danh sách");
        refreshButton.setOnAction(event -> {
            loadNextPageCode();
            refreshTable();
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(refreshButton, clearButton, saveButton);

        card.getChildren().addAll(cardTitle, gridPane, buttonBox);

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

        Label cardTitle = new Label("Danh sách fanpage");
        cardTitle.setStyle("""
                -fx-font-size: 18px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label noteLabel = new Label(
                "Cảnh báo: xóa fanpage là xóa thật toàn bộ source, video ID, batch, file record và folder raw/edited liên quan."
        );
        noteLabel.setWrapText(true);
        noteLabel.setStyle("""
                -fx-font-size: 13px;
                -fx-text-fill: #b91c1c;
                -fx-font-weight: bold;
                """);

        buildTableColumns();

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.setPlaceholder(new Label("Chưa có fanpage nào."));
        tableView.setPrefHeight(380);

        card.getChildren().addAll(cardTitle, noteLabel, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        return card;
    }

    private void buildTableColumns() {
        TableColumn<Fanpage, String> sttColumn = new TableColumn<>("STT");
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
        sttColumn.setPrefWidth(60);
        sttColumn.setMaxWidth(70);

        TableColumn<Fanpage, String> codeColumn = new TableColumn<>("Mã");
        codeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getPageCode()))
        );

        TableColumn<Fanpage, String> nameColumn = new TableColumn<>("Tên fanpage");
        nameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getPageName()))
        );
        nameColumn.setPrefWidth(220);

        TableColumn<Fanpage, String> urlColumn = new TableColumn<>("Link");
        urlColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getPageUrl()))
        );
        urlColumn.setPrefWidth(240);

        TableColumn<Fanpage, String> nicheColumn = new TableColumn<>("Niche");
        nicheColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getNiche()))
        );

        TableColumn<Fanpage, Number> defaultCountColumn = new TableColumn<>("Mặc định");
        defaultCountColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getDefaultVideoCount())
        );

        TableColumn<Fanpage, String> activeColumn = new TableColumn<>("Active");
        activeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().isActive() ? "Có" : "Không")
        );

        TableColumn<Fanpage, String> createdColumn = new TableColumn<>("Ngày tạo");
        createdColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getCreatedTime()))
        );
        createdColumn.setPrefWidth(160);

        TableColumn<Fanpage, Button> actionColumn = new TableColumn<>("Thao tác");
        actionColumn.setCellValueFactory(data -> {
            Fanpage fanpage = data.getValue();

            Button deleteButton = dangerButton("Xóa toàn bộ");
            deleteButton.setOnAction(event -> hardDeleteFanpage(fanpage));

            return new ReadOnlyObjectWrapper<>(deleteButton);
        });
        actionColumn.setPrefWidth(130);

        tableView.getColumns().setAll(
                sttColumn,
                codeColumn,
                nameColumn,
                urlColumn,
                nicheColumn,
                defaultCountColumn,
                activeColumn,
                createdColumn,
                actionColumn
        );
    }

    private void saveFanpage() {
        String pageCode = pageCodeField.getText().trim();
        String pageName = pageNameField.getText().trim();
        String pageUrl = pageUrlField.getText().trim();
        String niche = nicheField.getText().trim();
        String defaultCountText = defaultVideoCountField.getText().trim();

        if (pageName.isBlank()) {
            GuiAlert.warning("Thiếu tên fanpage", "M cần nhập tên fanpage trước.");
            pageNameField.requestFocus();
            return;
        }

        int defaultVideoCount;

        try {
            if (defaultCountText.isBlank()) {
                defaultVideoCount = 6;
            } else {
                defaultVideoCount = Integer.parseInt(defaultCountText);
            }
        } catch (NumberFormatException exception) {
            GuiAlert.warning("Số video không hợp lệ", "Số video mặc định phải là số nguyên, ví dụ 6.");
            defaultVideoCountField.requestFocus();
            return;
        }

        if (defaultVideoCount <= 0) {
            GuiAlert.warning("Số video không hợp lệ", "Số video mặc định phải lớn hơn 0.");
            defaultVideoCountField.requestFocus();
            return;
        }

        try {
            Fanpage fanpage = new Fanpage();
            fanpage.setPageCode(pageCode);
            fanpage.setPageName(pageName);
            fanpage.setPageUrl(pageUrl);
            fanpage.setNiche(niche);
            fanpage.setDefaultVideoCount(defaultVideoCount);

            fanpageRepository.save(fanpage);

            GuiAlert.info(
                    "Thêm fanpage thành công",
                    "Đã thêm: " + pageCode + " - " + pageName
            );

            clearForm();
            refreshTable();

        } catch (Exception exception) {
            GuiAlert.error("Không thể thêm fanpage", exception);
        }
    }

    private void hardDeleteFanpage(Fanpage fanpage) {
        if (fanpage == null) {
            return;
        }

        Alert warningAlert = new Alert(Alert.AlertType.CONFIRMATION);
        warningAlert.setTitle("auto-by-Hapro");
        warningAlert.setHeaderText("Cảnh báo xóa toàn bộ fanpage");
        warningAlert.setContentText(
                "M chuẩn bị xóa toàn bộ fanpage này:\n\n"
                        + fanpage.getPageCode() + " - " + fanpage.getPageName() + "\n\n"
                        + "Dữ liệu sẽ bị xóa:\n"
                        + "- Fanpage\n"
                        + "- Tất cả source của fanpage\n"
                        + "- Tất cả video ID đã tải\n"
                        + "- Batch / file record / upload record liên quan\n"
                        + "- Folder raw/edited nếu còn tồn tại\n\n"
                        + "Sau khi xóa sẽ không còn chống tải trùng cho fanpage này nữa."
        );

        Optional<ButtonType> warningResult = warningAlert.showAndWait();

        if (warningResult.isEmpty() || warningResult.get() != ButtonType.OK) {
            return;
        }

        TextInputDialog confirmDialog = new TextInputDialog();
        confirmDialog.setTitle("Xác nhận xóa fanpage");
        confirmDialog.setHeaderText("Nhập đúng mã fanpage để xác nhận");
        confirmDialog.setContentText("Nhập mã: " + fanpage.getPageCode());

        Optional<String> confirmText = confirmDialog.showAndWait();

        if (confirmText.isEmpty()) {
            return;
        }

        String typedCode = confirmText.get().trim();

        if (!fanpage.getPageCode().equals(typedCode)) {
            GuiAlert.warning(
                    "Mã xác nhận không đúng",
                    "M phải nhập đúng mã " + fanpage.getPageCode() + " thì tool mới xóa."
            );
            return;
        }

        try {
            FanpageDeleteResult result =
                    fanpageDeleteService.hardDeleteFanpage(fanpage);

            GuiAlert.info(
                    "Đã xóa toàn bộ fanpage",
                    "Fanpage: " + result.getPageCode() + " - " + result.getPageName()
                            + "\nDòng DB đã xóa: " + result.getDatabaseDeletedRows()
                            + "\nFolder đã xóa: " + result.getDeletedFolderCount()
                            + "\nFile đã xóa: " + result.getDeletedFileCount()
                            + "\n\n" + result.getMessage()
            );

            refreshTable();
            loadNextPageCode();

        } catch (Exception exception) {
            GuiAlert.error("Không thể xóa fanpage", exception);
        }
    }

    private void refreshTable() {
        try {
            List<Fanpage> fanpages = fanpageRepository.findAll();
            tableView.setItems(FXCollections.observableArrayList(fanpages));
        } catch (Exception exception) {
            GuiAlert.error("Không thể tải danh sách fanpage", exception);
        }
    }

    private void clearForm() {
        pageNameField.clear();
        pageUrlField.clear();
        nicheField.clear();
        defaultVideoCountField.setText("6");
        loadNextPageCode();
        pageNameField.requestFocus();
    }

    private void loadNextPageCode() {
        try {
            String nextPageCode = fanpageRepository.generateNextPageCode();
            pageCodeField.setText(nextPageCode);
        } catch (Exception exception) {
            pageCodeField.setText("");
            GuiAlert.error("Không thể tạo mã fanpage", exception);
        }
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.setStyle("""
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-text-fill: #374151;
                """);
        return label;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(38);
        button.setStyle("""
                -fx-background-color: #2563eb;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                """);
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(38);
        button.setStyle("""
                -fx-background-color: #e5e7eb;
                -fx-text-fill: #111827;
                -fx-font-size: 14px;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                """);
        return button;
    }

    private Button dangerButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(30);
        button.setStyle("""
                -fx-background-color: #dc2626;
                -fx-text-fill: white;
                -fx-font-size: 12px;
                -fx-font-weight: bold;
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
}