package com.hapro.autobyhapro.database;

import com.hapro.autobyhapro.config.AppPaths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        String dbPath = AppPaths.databaseFile().toAbsolutePath().toString();
        String url = "jdbc:sqlite:" + dbPath;

        Connection connection = DriverManager.getConnection(url);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }
}