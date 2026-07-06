package com.hapro.autobyhapro.config;

import java.io.InputStream;
import java.util.Properties;

public final class AppVersion {

    private static final String DEFAULT_APP_NAME = "AutoByHapro";
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String DEFAULT_PUBLISHER = "Hapro";

    public static final String APP_NAME;
    public static final String VERSION;
    public static final String PUBLISHER;
    public static final String DISPLAY_NAME;

    static {
        Properties properties = new Properties();

        try (InputStream inputStream =
                     AppVersion.class.getResourceAsStream("/version.properties")) {

            if (inputStream != null) {
                properties.load(inputStream);
            }

        } catch (Exception exception) {
            System.err.println(
                    "Không thể đọc version.properties, dùng version mặc định: "
                            + exception.getMessage()
            );
        }

        APP_NAME = readValue(
                properties,
                "app.name",
                DEFAULT_APP_NAME
        );

        VERSION = readValue(
                properties,
                "app.version",
                DEFAULT_VERSION
        );

        PUBLISHER = readValue(
                properties,
                "app.publisher",
                DEFAULT_PUBLISHER
        );

        DISPLAY_NAME = APP_NAME + " v" + VERSION;
    }

    private AppVersion() {
    }

    private static String readValue(
            Properties properties,
            String key,
            String defaultValue
    ) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }
}
