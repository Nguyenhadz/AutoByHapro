package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.license.LicenseCheckResult;
import com.hapro.autobyhapro.license.LicenseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import com.hapro.autobyhapro.config.AppVersion;

import java.util.ArrayList;
import java.util.List;

public class MainWindow {

    private final BorderPane root = new BorderPane();
    private final VBox menuBox = new VBox(8);
    private final VBox contentBox = new VBox(12);

    private final List<Button> protectedMenuButtons = new ArrayList<>();

    public MainWindow() {
        buildLayout();

        if (LicenseManager.checkLicense().isValid()) {
            showHome();
        } else {
            showLicense();
        }

        refreshLicenseMenuState();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildLayout() {
        root.setLeft(menuBox);
        root.setCenter(contentBox);

        menuBox.setPrefWidth(260);
        menuBox.setPadding(new Insets(16));
        menuBox.setStyle("""
                -fx-background-color: #1f2937;
                """);

        contentBox.setPadding(new Insets(24));
        contentBox.setStyle("""
                -fx-background-color: #f3f4f6;
                """);

        Label appTitle = new Label(AppVersion.APP_NAME);
        appTitle.setStyle("""
                -fx-text-fill: white;
                -fx-font-size: 22px;
                -fx-font-weight: bold;
                """);

        Label appSubTitle = new Label("Video Workflow Tool");
        appSubTitle.setStyle("""
        -fx-font-size: 12px;
        -fx-text-fill: #cbd5e1;
        """);

        Label versionLabel = new Label("v" + AppVersion.VERSION);
        versionLabel.setStyle("""
        -fx-font-size: 12px;
        -fx-text-fill: #94a3b8;
        """);

        Button licenseButton = menuButton("🔐 Licence / Kích hoạt", this::showLicense);

        menuBox.getChildren().addAll(
                appTitle,
                appSubTitle,
                spacer(10),
                licenseButton,
                spacer(6),
                protectedMenuButton("🏠 Trang chủ", this::showHome),
                protectedMenuButton("⬇ Kế hoạch download", this::showDownloadPlan),
                protectedMenuButton("📄 Quản lý fanpage", this::showFanpage),
                protectedMenuButton("🔗 Quản lý source", this::showSource),
                protectedMenuButton("✅ Kiểm tra source", this::showCheckSource),
                protectedMenuButton("🎬 Phân loại video edit", this::showSortEditedVideo),
                protectedMenuButton("📤 Upload thủ công", this::showManualUpload),
                protectedMenuButton("🧰 Công cụ hệ thống", this::showSystemTools),
                protectedMenuButton("📊 Thống kê", this::showStats)
        );
    }

    private Button protectedMenuButton(String text, Runnable action) {
        Button button = menuButton(text, () -> runIfLicensed(action));
        protectedMenuButtons.add(button);
        return button;
    }

    private void runIfLicensed(Runnable action) {
        LicenseCheckResult result = LicenseManager.checkLicense();

        if (result.isValid()) {
            action.run();
            refreshLicenseMenuState();
            return;
        }

        refreshLicenseMenuState();

        GuiAlert.warning(
                "Tool chưa được kích hoạt",
                result.getMessage()
        );

        showLicense();
    }

    private void refreshLicenseMenuState() {
        boolean valid = LicenseManager.checkLicense().isValid();

        for (Button button : protectedMenuButtons) {
            button.setDisable(!valid);
            button.setOpacity(valid ? 1.0 : 0.45);
        }
    }

    private Button menuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(42);
        button.setAlignment(Pos.CENTER_LEFT);

        applyNormalButtonStyle(button);

        button.setOnMouseEntered(event -> {
            if (!button.isDisabled()) {
                applyHoverButtonStyle(button);
            }
        });

        button.setOnMouseExited(event -> applyNormalButtonStyle(button));

        button.setOnAction(event -> action.run());

        return button;
    }

    private void applyNormalButtonStyle(Button button) {
        button.setStyle("""
                -fx-background-color: #374151;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                """);
    }

    private void applyHoverButtonStyle(Button button) {
        button.setStyle("""
                -fx-background-color: #4b5563;
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                """);
    }

    private Label spacer(int height) {
        Label label = new Label("");
        label.setMinHeight(height);
        return label;
    }

    private void setPlaceholderContent(String title, String description) {
        contentBox.getChildren().clear();

        Label titleLabel = new Label(title);
        titleLabel.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #111827;
                """);

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("""
                -fx-font-size: 15px;
                -fx-text-fill: #4b5563;
                """);

        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 12;
                -fx-border-color: #e5e7eb;
                -fx-border-radius: 12;
                """);

        VBox.setVgrow(card, Priority.ALWAYS);

        card.getChildren().addAll(titleLabel, descriptionLabel);
        contentBox.getChildren().add(card);
    }

    private void setView(Parent view) {
        contentBox.getChildren().clear();
        contentBox.getChildren().add(view);
        VBox.setVgrow(view, Priority.ALWAYS);
    }

    private void showLicense() {
        LicenseView licenseView = new LicenseView(() -> {
            refreshLicenseMenuState();
            showHome();
        });

        setView(licenseView.getRoot());
        refreshLicenseMenuState();
    }

    private void showHome() {
        HomeView homeView = new HomeView();
        setView(homeView.getRoot());
    }

    private void showDownloadPlan() {
        DownloadPlanView downloadPlanView = new DownloadPlanView();
        setView(downloadPlanView.getRoot());
    }

    private void showFanpage() {
        FanpageView fanpageView = new FanpageView();
        setView(fanpageView.getRoot());
    }

    private void showSource() {
        SourceView sourceView = new SourceView();
        setView(sourceView.getRoot());
    }

    private void showCheckSource() {
        CheckSourceView checkSourceView = new CheckSourceView();
        setView(checkSourceView.getRoot());
    }

    private void showSortEditedVideo() {
        SortEditedVideoView sortEditedVideoView = new SortEditedVideoView();
        setView(sortEditedVideoView.getRoot());
    }

    private void showManualUpload() {
        ManualUploadView manualUploadView = new ManualUploadView();
        setView(manualUploadView.getRoot());
    }

    private void showSystemTools() {
        SystemToolsView systemToolsView = new SystemToolsView();
        setView(systemToolsView.getRoot());
    }

    private void showStats() {
        StatsView statsView = new StatsView();
        setView(statsView.getRoot());
    }
}
