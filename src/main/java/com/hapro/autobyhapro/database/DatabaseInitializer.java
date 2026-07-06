package com.hapro.autobyhapro.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        String sql = loadInitSql();

        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement()) {

            String[] commands = sql.split(";");

            for (String command : commands) {
                String cleanCommand = command.trim();

                if (!cleanCommand.isEmpty()) {
                    statement.execute(cleanCommand);
                }
            }

            System.out.println("Database initialized successfully.");

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to initialize database.", exception);
        }
    }

    private static String loadInitSql() {
        try (InputStream inputStream = DatabaseInitializer.class.getResourceAsStream("/sql/init.sql")) {

            if (inputStream == null) {
                throw new RuntimeException("Cannot find /sql/init.sql");
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException exception) {
            throw new RuntimeException("Failed to read init.sql.", exception);
        }
    }
}