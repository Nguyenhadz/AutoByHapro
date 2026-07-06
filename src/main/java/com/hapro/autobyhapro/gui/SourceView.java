package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.entity.DeletedSourceInfo;
import com.hapro.autobyhapro.entity.Fanpage;
import com.hapro.autobyhapro.entity.Source;
import com.hapro.autobyhapro.repository.DeleteManagementRepository;
import com.hapro.autobyhapro.repository.FanpageRepository;
import com.hapro.autobyhapro.repository.SourceRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SourceView {

    private static final String SOURCE_TYPE_YOUTUBE = "YOUTUBE";
    private static final String SOURCE_TYPE_TIKTOK = "TIKTOK";

    private static final String SOURCE_OPTION_YOUTUBE_SHORT = "YOUTUBE - SHORT";
    private static final String SOURCE_OPTION_YOUTUBE_LONG = "YOUTUBE - Video dài";
    private static final String SOURCE_OPTION_TIKTOK = "TIKTOK";

    private final FanpageRepository fanpageRepository = new FanpageRepository();
    private final SourceRepository sourceRepository = new SourceRepository();
    private final DeleteManagementRepository deleteManagementRepository = new DeleteManagementRepository();

    private final ScrollPane rootScrollPane = new ScrollPane();
    private final VBox root = new VBox(16);

    private final TableView<Source> tableView = new TableView<>();
    private final TableView<DeletedSourceInfo> deletedSourceTableView = new TableView<>();

    private final ComboBox<Fanpage> fanpageComboBox = new ComboBox<>();
    private final TextField sourceCodeField = new TextField();
    private final ChoiceBox<String> sourceTypeChoiceBox = new ChoiceBox<>();
    private final TextField sourceNameField = new TextField();
    private final TextField sourceUrlField = new TextField();
    private final TextField channelNameField = new TextField();

    public SourceView() {
        buildLayout();
        loadFanpages();
        loadNextSourceCode();
        refreshAllTables();
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

        Label titleLabel = new Label("Quản lý source");
        titleLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(
                "Thêm source YouTube/TikTok, chọn riêng YouTube Shorts hoặc video dài, "
                        + "xóa source không active và xem lại source đã từng xóa."
        );
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("""
                -fx-font-size: 14px;
                -fx-text-fill: #4b5563;
                """);

        VBox formCard = buildFormCard();
        VBox tableCard = buildTableCard();
        VBox deletedSourceCard = buildDeletedSourceTableCard();

        root.getChildren().addAll(
                titleLabel,
                descriptionLabel,
                formCard,
                tableCard,
                deletedSourceCard
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

        Label cardTitle = new Label("Thêm source mới");
        cardTitle.setStyle("""
                -fx-font-size: 18px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        setupFanpageComboBox();
        setupSourceTypeControls();

        sourceCodeField.setEditable(false);
        sourceCodeField.setPromptText("Tự động tạo, ví dụ S001");

        sourceNameField.setPromptText("Ví dụ: MrBeast Shorts");
        channelNameField.setPromptText("Có thể bỏ trống");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(14);
        gridPane.setVgap(12);

        ColumnConstraints labelColumn1 = new ColumnConstraints();
        labelColumn1.setMinWidth(120);

        ColumnConstraints fieldColumn1 = new ColumnConstraints();
        fieldColumn1.setHgrow(Priority.ALWAYS);
        fieldColumn1.setPercentWidth(35);

        ColumnConstraints labelColumn2 = new ColumnConstraints();
        labelColumn2.setMinWidth(120);

        ColumnConstraints fieldColumn2 = new ColumnConstraints();
        fieldColumn2.setHgrow(Priority.ALWAYS);
        fieldColumn2.setPercentWidth(35);

        gridPane.getColumnConstraints().addAll(
                labelColumn1,
                fieldColumn1,
                labelColumn2,
                fieldColumn2
        );

        gridPane.add(label("Fanpage *"), 0, 0);
        gridPane.add(fanpageComboBox, 1, 0);

        gridPane.add(label("Mã source"), 2, 0);
        gridPane.add(sourceCodeField, 3, 0);

        gridPane.add(label("Loại source *"), 0, 1);
        gridPane.add(sourceTypeChoiceBox, 1, 1);

        gridPane.add(label("Tên source *"), 2, 1);
        gridPane.add(sourceNameField, 3, 1);

        gridPane.add(label("Link source *"), 0, 2);
        gridPane.add(sourceUrlField, 1, 2, 3, 1);

        gridPane.add(label("Tên kênh hiển thị"), 0, 3);
        gridPane.add(channelNameField, 1, 3, 3, 1);

        fanpageComboBox.setMaxWidth(Double.MAX_VALUE);
        sourceCodeField.setMaxWidth(Double.MAX_VALUE);
        sourceTypeChoiceBox.setMaxWidth(Double.MAX_VALUE);
        sourceNameField.setMaxWidth(Double.MAX_VALUE);
        sourceUrlField.setMaxWidth(Double.MAX_VALUE);
        channelNameField.setMaxWidth(Double.MAX_VALUE);

        Button saveButton = primaryButton("Thêm source");
        saveButton.setOnAction(event -> saveSource());

        Button clearButton = secondaryButton("Làm mới form");
        clearButton.setOnAction(event -> clearForm());

        Button refreshButton = secondaryButton("Refresh");
        refreshButton.setOnAction(event -> {
            loadFanpages();
            loadNextSourceCode();
            refreshAllTables();
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(refreshButton, clearButton, saveButton);

        Label noteLabel = new Label(
                "Chọn trực tiếp YOUTUBE - SHORT, YOUTUBE - Video dài hoặc TIKTOK trong ô Loại source. "
                        + "Nếu dán link kênh YouTube chung hoặc dán nhầm tab /shorts và /videos, "
                        + "tool sẽ tự chuyển link theo loại đã chọn trước khi lưu. "
                        + "Khi thêm source mới cho một fanpage, source active cũ sẽ tự chuyển thành Active = Không."
        );
        noteLabel.setWrapText(true);
        noteLabel.setStyle("""
                -fx-font-size: 13px;
                -fx-text-fill: #6b7280;
                """);

        card.getChildren().addAll(cardTitle, gridPane, noteLabel, buttonBox);

        return card;
    }

    private void setupSourceTypeControls() {
        sourceTypeChoiceBox.setItems(
                FXCollections.observableArrayList(
                        SOURCE_OPTION_YOUTUBE_SHORT,
                        SOURCE_OPTION_YOUTUBE_LONG,
                        SOURCE_OPTION_TIKTOK
                )
        );

        sourceTypeChoiceBox.setValue(SOURCE_OPTION_YOUTUBE_SHORT);

        sourceTypeChoiceBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    updateSourceUrlPrompt();
                    normalizeYoutubeUrlInField(false);
                });

        sourceUrlField.focusedProperty().addListener(
                (observable, oldValue, focused) -> {
                    if (!focused) {
                        normalizeYoutubeUrlInField(false);
                    }
                }
        );

        sourceUrlField.setOnAction(event ->
                normalizeYoutubeUrlInField(true)
        );

        updateSourceUrlPrompt();
    }

    private void updateSourceUrlPrompt() {
        String selectedSourceOption = sourceTypeChoiceBox.getValue();

        if (SOURCE_OPTION_TIKTOK.equalsIgnoreCase(selectedSourceOption)) {
            sourceUrlField.setPromptText(
                    "Ví dụ: https://www.tiktok.com/@tenkenh"
            );
            return;
        }

        if (isYoutubeLongOption(selectedSourceOption)) {
            sourceUrlField.setPromptText(
                    "Ví dụ: https://www.youtube.com/@tenkenh/videos"
            );
            return;
        }

        sourceUrlField.setPromptText(
                "Ví dụ: https://www.youtube.com/@tenkenh/shorts"
        );
    }

    private boolean isYoutubeOption(String selectedSourceOption) {
        return SOURCE_OPTION_YOUTUBE_SHORT.equalsIgnoreCase(selectedSourceOption)
                || SOURCE_OPTION_YOUTUBE_LONG.equalsIgnoreCase(selectedSourceOption);
    }

    private boolean isYoutubeLongOption(String selectedSourceOption) {
        return SOURCE_OPTION_YOUTUBE_LONG.equalsIgnoreCase(selectedSourceOption);
    }

    private String toDatabaseSourceType(String selectedSourceOption) {
        if (isYoutubeOption(selectedSourceOption)) {
            return SOURCE_TYPE_YOUTUBE;
        }

        if (SOURCE_OPTION_TIKTOK.equalsIgnoreCase(selectedSourceOption)) {
            return SOURCE_TYPE_TIKTOK;
        }

        return "";
    }

    private boolean normalizeYoutubeUrlInField(boolean showWarning) {
        String selectedSourceOption = sourceTypeChoiceBox.getValue();

        if (!isYoutubeOption(selectedSourceOption)) {
            return true;
        }

        String currentUrl = sourceUrlField.getText();

        if (currentUrl == null || currentUrl.isBlank()) {
            return true;
        }

        try {
            String normalizedUrl = normalizeYoutubeChannelUrl(
                    currentUrl,
                    selectedSourceOption
            );

            sourceUrlField.setText(normalizedUrl);
            return true;

        } catch (IllegalArgumentException exception) {
            if (showWarning) {
                GuiAlert.warning(
                        "Link YouTube chưa hợp lệ",
                        exception.getMessage()
                );
                sourceUrlField.requestFocus();
            }

            return false;
        }
    }

    private String normalizeYoutubeChannelUrl(
            String rawUrl,
            String selectedSourceOption
    ) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("Link YouTube đang bị trống.");
        }

        if (!isYoutubeOption(selectedSourceOption)) {
            throw new IllegalArgumentException(
                    "M cần chọn YOUTUBE - SHORT hoặc YOUTUBE - Video dài."
            );
        }

        String workingUrl = rawUrl.trim();

        if (!workingUrl.matches("(?i)^https?://.*")) {
            workingUrl = "https://" + workingUrl;
        }

        URI uri;

        try {
            uri = URI.create(workingUrl);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Link YouTube không đúng định dạng.");
        }

        String host = uri.getHost();

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Không đọc được tên miền từ link YouTube.");
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);

        boolean validYoutubeHost = lowerHost.equals("youtube.com")
                || lowerHost.equals("www.youtube.com")
                || lowerHost.equals("m.youtube.com");

        if (!validYoutubeHost) {
            if (lowerHost.equals("youtu.be") || lowerHost.endsWith(".youtu.be")) {
                throw new IllegalArgumentException(
                        "Đây là link video rút gọn, không phải link kênh. "
                                + "Hãy mở trang kênh rồi copy link kênh."
                );
            }

            throw new IllegalArgumentException("Link không thuộc youtube.com.");
        }

        List<String> pathSegments = splitPathSegments(uri.getPath());

        if (pathSegments.isEmpty()) {
            throw new IllegalArgumentException(
                    "Link mới chỉ tới trang chủ YouTube. Hãy nhập link của một kênh cụ thể."
            );
        }

        String firstSegment = pathSegments.get(0);
        String lowerFirstSegment = firstSegment.toLowerCase(Locale.ROOT);

        if (isSpecificYoutubeContentPath(lowerFirstSegment)) {
            throw new IllegalArgumentException(
                    "Đây là link video, Shorts, playlist hoặc nội dung cụ thể; "
                            + "không phải link kênh. Hãy copy link trang kênh."
            );
        }

        String channelBasePath;

        if (firstSegment.startsWith("@")) {
            channelBasePath = "/" + firstSegment;

        } else if (lowerFirstSegment.equals("channel")
                || lowerFirstSegment.equals("c")
                || lowerFirstSegment.equals("user")) {

            if (pathSegments.size() < 2 || pathSegments.get(1).isBlank()) {
                throw new IllegalArgumentException(
                        "Link kênh YouTube đang thiếu mã hoặc tên kênh."
                );
            }

            channelBasePath = "/" + firstSegment + "/" + pathSegments.get(1);

        } else {
            // Hỗ trợ một số link kênh dạng cũ: youtube.com/TenKenh
            channelBasePath = "/" + firstSegment;
        }

        String tabPath = isYoutubeLongOption(selectedSourceOption)
                ? "/videos"
                : "/shorts";

        return "https://www.youtube.com" + channelBasePath + tabPath;
    }

    private List<String> splitPathSegments(String path) {
        List<String> segments = new ArrayList<>();

        if (path == null || path.isBlank()) {
            return segments;
        }

        String[] rawSegments = path.split("/");

        for (String segment : rawSegments) {
            if (segment != null && !segment.isBlank()) {
                segments.add(segment.trim());
            }
        }

        return segments;
    }

    private boolean isSpecificYoutubeContentPath(String lowerFirstSegment) {
        return lowerFirstSegment.equals("watch")
                || lowerFirstSegment.equals("shorts")
                || lowerFirstSegment.equals("playlist")
                || lowerFirstSegment.equals("live")
                || lowerFirstSegment.equals("embed")
                || lowerFirstSegment.equals("results")
                || lowerFirstSegment.equals("feed");
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

        Label cardTitle = new Label("Danh sách source hiện tại");
        cardTitle.setStyle("""
                -fx-font-size: 18px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label noteLabel = new Label("Chỉ source Active = Không mới xóa được. Khi xóa, tool sẽ lưu tên kênh và link vào bảng source đã xóa bên dưới.");
        noteLabel.setWrapText(true);
        noteLabel.setStyle("""
                -fx-font-size: 13px;
                -fx-text-fill: #6b7280;
                """);

        buildTableColumns();

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.setPlaceholder(new Label("Chưa có source nào."));
        tableView.setPrefHeight(330);

        card.getChildren().addAll(cardTitle, noteLabel, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        return card;
    }

    private VBox buildDeletedSourceTableCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(18));
        card.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 12;
                -fx-border-color: #e5e7eb;
                -fx-border-radius: 12;
                """);

        Label cardTitle = new Label("Source đã xóa");
        cardTitle.setStyle("""
                -fx-font-size: 18px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label noteLabel = new Label("Danh sách này dùng để xem lại nguồn video nào đã từng sử dụng nhưng không hiệu quả.");
        noteLabel.setWrapText(true);
        noteLabel.setStyle("""
                -fx-font-size: 13px;
                -fx-text-fill: #6b7280;
                """);

        buildDeletedSourceTableColumns();

        deletedSourceTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        deletedSourceTableView.setPlaceholder(new Label("Chưa có source đã xóa."));
        deletedSourceTableView.setPrefHeight(260);

        card.getChildren().addAll(cardTitle, noteLabel, deletedSourceTableView);

        return card;
    }

    private void setupFanpageComboBox() {
        fanpageComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Fanpage fanpage) {
                if (fanpage == null) {
                    return "";
                }

                return fanpage.getPageCode() + " - " + fanpage.getPageName();
            }

            @Override
            public Fanpage fromString(String text) {
                return null;
            }
        });
    }

    private void buildTableColumns() {
        TableColumn<Source, String> sttColumn = new TableColumn<>("STT");
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

        TableColumn<Source, String> sourceCodeColumn = new TableColumn<>("Source");
        sourceCodeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getSourceCode()))
        );

        TableColumn<Source, String> pageCodeColumn = new TableColumn<>("Page");
        pageCodeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getFanpageCode()))
        );

        TableColumn<Source, String> pageNameColumn = new TableColumn<>("Tên fanpage");
        pageNameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getFanpageName()))
        );
        pageNameColumn.setPrefWidth(180);

        TableColumn<Source, String> typeColumn = new TableColumn<>("Loại");
        typeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(displaySourceType(data.getValue()))
        );
        typeColumn.setPrefWidth(170);

        TableColumn<Source, String> nameColumn = new TableColumn<>("Tên source");
        nameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getSourceName()))
        );
        nameColumn.setPrefWidth(200);

        TableColumn<Source, String> channelColumn = new TableColumn<>("Tên kênh");
        channelColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getChannelName()))
        );
        channelColumn.setPrefWidth(160);

        TableColumn<Source, String> urlColumn = new TableColumn<>("Link");
        urlColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getSourceUrl()))
        );
        urlColumn.setPrefWidth(260);

        TableColumn<Source, String> activeColumn = new TableColumn<>("Active");
        activeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().isActive() ? "Có" : "Không")
        );

        TableColumn<Source, Button> actionColumn = new TableColumn<>("Thao tác");
        actionColumn.setCellValueFactory(data -> {
            Source source = data.getValue();

            Button deleteButton = dangerButton("Xóa");

            if (source.isActive()) {
                deleteButton.setText("Đang active");
                deleteButton.setDisable(true);
            } else {
                deleteButton.setOnAction(event -> deleteInactiveSource(source));
            }

            return new ReadOnlyObjectWrapper<>(deleteButton);
        });
        actionColumn.setPrefWidth(120);

        tableView.getColumns().setAll(
                sttColumn,
                sourceCodeColumn,
                pageCodeColumn,
                pageNameColumn,
                typeColumn,
                nameColumn,
                channelColumn,
                urlColumn,
                activeColumn,
                actionColumn
        );
    }

    private String displaySourceType(Source source) {
        if (source == null) {
            return "";
        }

        String sourceType = nullToEmpty(source.getSourceType());

        if (!SOURCE_TYPE_YOUTUBE.equalsIgnoreCase(sourceType)) {
            return sourceType;
        }

        String sourceUrl = nullToEmpty(source.getSourceUrl())
                .toLowerCase(Locale.ROOT);

        if (sourceUrl.matches(".*/shorts/?(?:\\?.*)?$")) {
            return SOURCE_OPTION_YOUTUBE_SHORT;
        }

        if (sourceUrl.matches(".*/videos/?(?:\\?.*)?$")) {
            return SOURCE_OPTION_YOUTUBE_LONG;
        }

        return "YOUTUBE";
    }

    private void buildDeletedSourceTableColumns() {
        TableColumn<DeletedSourceInfo, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getId())
        );
        idColumn.setMaxWidth(70);

        TableColumn<DeletedSourceInfo, String> pageColumn = new TableColumn<>("Page");
        pageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(
                        nullToEmpty(data.getValue().getPageCode())
                                + " - "
                                + nullToEmpty(data.getValue().getPageName())
                )
        );
        pageColumn.setPrefWidth(180);

        TableColumn<DeletedSourceInfo, String> oldSourceCodeColumn = new TableColumn<>("Source cũ");
        oldSourceCodeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getOldSourceCode()))
        );

        TableColumn<DeletedSourceInfo, String> typeColumn = new TableColumn<>("Loại");
        typeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getSourceType()))
        );

        TableColumn<DeletedSourceInfo, String> sourceNameColumn = new TableColumn<>("Tên source");
        sourceNameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getSourceName()))
        );
        sourceNameColumn.setPrefWidth(200);

        TableColumn<DeletedSourceInfo, String> channelNameColumn = new TableColumn<>("Tên kênh");
        channelNameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getChannelName()))
        );
        channelNameColumn.setPrefWidth(160);

        TableColumn<DeletedSourceInfo, String> urlColumn = new TableColumn<>("Link");
        urlColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getSourceUrl()))
        );
        urlColumn.setPrefWidth(260);

        TableColumn<DeletedSourceInfo, String> deletedTimeColumn = new TableColumn<>("Thời gian xóa");
        deletedTimeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullToEmpty(data.getValue().getDeletedTime()))
        );
        deletedTimeColumn.setPrefWidth(160);

        deletedSourceTableView.getColumns().setAll(
                idColumn,
                pageColumn,
                oldSourceCodeColumn,
                typeColumn,
                sourceNameColumn,
                channelNameColumn,
                urlColumn,
                deletedTimeColumn
        );
    }

    private void saveSource() {
        Fanpage selectedFanpage = fanpageComboBox.getValue();

        if (selectedFanpage == null) {
            GuiAlert.warning("Thiếu fanpage", "M cần chọn fanpage trước.");
            fanpageComboBox.requestFocus();
            return;
        }

        String sourceCode = sourceCodeField.getText().trim();
        String selectedSourceOption = sourceTypeChoiceBox.getValue();
        String sourceName = sourceNameField.getText().trim();
        String sourceUrl = sourceUrlField.getText().trim();
        String channelName = channelNameField.getText().trim();

        if (selectedSourceOption == null || selectedSourceOption.isBlank()) {
            GuiAlert.warning(
                    "Thiếu loại source",
                    "M cần chọn YOUTUBE - SHORT, YOUTUBE - Video dài hoặc TIKTOK."
            );
            sourceTypeChoiceBox.requestFocus();
            return;
        }

        String sourceType = toDatabaseSourceType(selectedSourceOption);

        if (sourceType.isBlank()) {
            GuiAlert.warning(
                    "Loại source không hợp lệ",
                    "M hãy chọn lại loại source trong danh sách."
            );
            sourceTypeChoiceBox.requestFocus();
            return;
        }

        if (sourceName.isBlank()) {
            GuiAlert.warning("Thiếu tên source", "M cần nhập tên source.");
            sourceNameField.requestFocus();
            return;
        }

        if (sourceUrl.isBlank()) {
            GuiAlert.warning("Thiếu link source", "M cần nhập link source.");
            sourceUrlField.requestFocus();
            return;
        }

        if (isYoutubeOption(selectedSourceOption)) {
            boolean normalized = normalizeYoutubeUrlInField(true);

            if (!normalized) {
                return;
            }

            sourceUrl = sourceUrlField.getText().trim();
        }

        try {
            Source source = new Source();
            source.setFanpageId(selectedFanpage.getId());
            source.setSourceCode(sourceCode);
            source.setSourceType(sourceType);
            source.setSourceName(sourceName);
            source.setSourceUrl(sourceUrl);
            source.setChannelName(channelName);

            sourceRepository.saveNewActiveSource(source);

            GuiAlert.info(
                    "Thêm source thành công",
                    "Đã thêm source " + sourceCode + " cho fanpage "
                            + selectedFanpage.getPageCode()
                            + "\n\nLink đã lưu:\n"
                            + sourceUrl
            );

            clearForm();
            refreshAllTables();

        } catch (Exception exception) {
            GuiAlert.error("Không thể thêm source", exception);
        }
    }

    private void deleteInactiveSource(Source source) {
        if (source == null) {
            return;
        }

        if (source.isActive()) {
            GuiAlert.warning(
                    "Không thể xóa source đang active",
                    "Source đang Active = Có. Hãy thêm source mới cho fanpage này trước, source cũ sẽ thành Active = Không rồi mới xóa được."
            );
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("auto-by-Hapro");
        confirmAlert.setHeaderText("Xác nhận xóa source");
        confirmAlert.setContentText(
                "Source này sẽ bị xóa khỏi danh sách hiện tại, nhưng tên kênh và link sẽ được lưu vào Source đã xóa.\n\n"
                        + "Source: " + nullToEmpty(source.getSourceCode()) + "\n"
                        + "Tên: " + nullToEmpty(source.getSourceName()) + "\n"
                        + "Link: " + nullToEmpty(source.getSourceUrl())
        );

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            int deletedCount = deleteManagementRepository.deleteInactiveSourceToHistory(source.getId());

            GuiAlert.info(
                    "Đã xóa source",
                    "Đã xóa " + deletedCount + " source và lưu vào danh sách Source đã xóa."
            );

            refreshAllTables();

        } catch (Exception exception) {
            GuiAlert.error("Không thể xóa source", exception);
        }
    }

    private void loadFanpages() {
        try {
            List<Fanpage> fanpages = fanpageRepository.findAll()
                    .stream()
                    .filter(Fanpage::isActive)
                    .toList();

            fanpageComboBox.setItems(FXCollections.observableArrayList(fanpages));

            if (!fanpages.isEmpty() && fanpageComboBox.getValue() == null) {
                fanpageComboBox.setValue(fanpages.get(0));
            }

        } catch (Exception exception) {
            GuiAlert.error("Không thể tải danh sách fanpage", exception);
        }
    }

    private void refreshAllTables() {
        refreshSourceTable();
        refreshDeletedSourceTable();
    }

    private void refreshSourceTable() {
        try {
            List<Long> deletedSourceIds = deleteManagementRepository.findDeletedSources()
                    .stream()
                    .map(DeletedSourceInfo::getOldSourceId)
                    .filter(id -> id != null)
                    .toList();

            List<Source> sources = sourceRepository.findAll()
                    .stream()
                    .filter(source -> source.getId() == null || !deletedSourceIds.contains(source.getId()))
                    .toList();

            tableView.setItems(FXCollections.observableArrayList(sources));

        } catch (Exception exception) {
            GuiAlert.error("Không thể tải danh sách source", exception);
        }
    }

    private void refreshDeletedSourceTable() {
        try {
            List<DeletedSourceInfo> deletedSources = deleteManagementRepository.findDeletedSources();
            deletedSourceTableView.setItems(FXCollections.observableArrayList(deletedSources));
        } catch (Exception exception) {
            GuiAlert.error("Không thể tải danh sách source đã xóa", exception);
        }
    }

    private void clearForm() {
        sourceNameField.clear();
        sourceUrlField.clear();
        channelNameField.clear();
        sourceTypeChoiceBox.setValue(SOURCE_OPTION_YOUTUBE_SHORT);
        updateSourceUrlPrompt();
        loadNextSourceCode();

        if (!fanpageComboBox.getItems().isEmpty() && fanpageComboBox.getValue() == null) {
            fanpageComboBox.setValue(fanpageComboBox.getItems().get(0));
        }

        sourceNameField.requestFocus();
    }

    private void loadNextSourceCode() {
        try {
            String nextSourceCode = sourceRepository.generateNextSourceCode();
            sourceCodeField.setText(nextSourceCode);
        } catch (Exception exception) {
            sourceCodeField.setText("");
            GuiAlert.error("Không thể tạo mã source", exception);
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