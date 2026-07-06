package com.hapro.autobyhapro.gui;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.database.DatabaseInitializer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.hapro.autobyhapro.config.AppVersion;

import java.io.InputStream;

public class GuiMain extends Application {

    @Override
    public void start(Stage stage) {
        try {
            AppPaths.ensureBaseDirectories();
            DatabaseInitializer.initialize();

            MainWindow mainWindow = new MainWindow();

            Scene scene = new Scene(
                    mainWindow.getRoot(),
                    1200,
                    760
            );
            loadTableStyle(scene);

            stage.setTitle(AppVersion.DISPLAY_NAME);
            loadWindowIcon(stage);

            stage.setScene(scene);
            stage.setMinWidth(1050);
            stage.setMinHeight(680);
            stage.show();

        } catch (Exception exception) {
            ErrorWindow.showFatalError(exception);
        }
    }

    private void loadWindowIcon(Stage stage) {
        try {
            InputStream iconStream = getClass().getResourceAsStream("/icons/app.jpg");

            if (iconStream == null) {
                System.out.println("Không tìm thấy icon cửa sổ: /icons/app.jpg");
                return;
            }

            stage.getIcons().add(new Image(iconStream));

        } catch (Exception exception) {
            System.out.println("Không load được icon cửa sổ: " + exception.getMessage());
        }
    }

    private void loadTableStyle(Scene scene) {
        try {
            String css = GuiMain.class
                    .getResource("/styles/table-style.css")
                    .toExternalForm();

            scene.getStylesheets().add(css);

        } catch (Exception exception) {
            System.out.println("Không load được CSS bảng: " + exception.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}