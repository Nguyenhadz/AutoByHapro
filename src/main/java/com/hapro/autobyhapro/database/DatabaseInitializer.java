package com.hapro.autobyhapro.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
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

            migrateDatabase(connection);

            System.out.println("Database initialized successfully.");

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to initialize database.", exception);
        }
    }

    private static void migrateDatabase(Connection connection) throws SQLException {
        migrateSourcesTable(connection);
    }

    private static void migrateSourcesTable(Connection connection) throws SQLException {
        if (!tableExists(connection, "sources")) {
            return;
        }

        addColumnIfMissing(connection, "sources", "tiktok_channel_id", "TEXT");
        addColumnIfMissing(connection, "sources", "resolved_source_url", "TEXT");
        addColumnIfMissing(connection, "sources", "resolved_time", "TEXT");
        addColumnIfMissing(connection, "sources", "resolved_status", "TEXT");
        addColumnIfMissing(connection, "sources", "resolve_message", "TEXT");
    }

    private static void addColumnIfMissing(
            Connection connection,
            String tableName,
            String columnName,
            String columnDefinition
    ) throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE "
                            + tableName
                            + " ADD COLUMN "
                            + columnName
                            + " "
                            + columnDefinition
            );
        }
    }

    private static boolean tableExists(
            Connection connection,
            String tableName
    ) throws SQLException {
        String sql = """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table'
                  AND name = ?
                LIMIT 1
                """;

        try (var preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, tableName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static boolean columnExists(
            Connection connection,
            String tableName,
            String columnName
    ) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                String currentColumnName = resultSet.getString("name");

                if (columnName.equalsIgnoreCase(currentColumnName)) {
                    return true;
                }
            }

            return false;
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
