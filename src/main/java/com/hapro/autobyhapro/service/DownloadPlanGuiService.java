package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.DownloadPlanGuiRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DownloadPlanGuiService {

    public List<DownloadPlanGuiRow> loadRows() {
        try (Connection connection = DatabaseManager.getConnection()) {
            if (!tableExists(connection, "fanpages")) {
                return List.of();
            }

            boolean sourcesTableExists = tableExists(connection, "sources");
            boolean deletedSourcesTableExists = tableExists(connection, "deleted_sources");

            StringBuilder sqlBuilder = new StringBuilder();

            sqlBuilder.append("""
                    SELECT
                        f.id AS fanpage_id,
                        f.page_code,
                        f.page_name,
                        f.default_video_count
                    """);

            if (sourcesTableExists) {
                sqlBuilder.append("""
                        ,
                        s.id AS source_id,
                        s.source_code,
                        s.source_name,
                        s.source_type,
                        s.source_url
                        """);
            } else {
                sqlBuilder.append("""
                        ,
                        NULL AS source_id,
                        '' AS source_code,
                        '' AS source_name,
                        '' AS source_type,
                        '' AS source_url
                        """);
            }

            sqlBuilder.append("""
                    FROM fanpages f
                    """);

            if (sourcesTableExists) {
                sqlBuilder.append("""
                        LEFT JOIN sources s
                            ON s.fanpage_id = f.id
                           AND s.active = 1
                        """);

                if (deletedSourcesTableExists) {
                    sqlBuilder.append("""
                           AND NOT EXISTS (
                               SELECT 1
                               FROM deleted_sources ds
                               WHERE ds.old_source_id = s.id
                           )
                        """);
                }
            }

            sqlBuilder.append("""
                    WHERE f.active = 1
                    ORDER BY f.page_code ASC
                    """);

            List<DownloadPlanGuiRow> rows = new ArrayList<>();

            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlBuilder.toString());
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    DownloadPlanGuiRow row = new DownloadPlanGuiRow(
                            resultSet.getLong("fanpage_id"),
                            resultSet.getString("page_code"),
                            resultSet.getString("page_name"),
                            readNullableLong(resultSet, "source_id"),
                            resultSet.getString("source_code"),
                            resultSet.getString("source_name"),
                            resultSet.getString("source_type"),
                            resultSet.getString("source_url"),
                            resultSet.getInt("default_video_count")
                    );

                    rows.add(row);
                }
            }

            return rows;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tải kế hoạch download.", exception);
        }
    }

    public String buildPlanText(List<DownloadPlanGuiRow> rows, int threadCount) {
        if (rows == null || rows.isEmpty()) {
            return "Chưa chọn fanpage nào để download.";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("KẾ HOẠCH DOWNLOAD")
                .append(System.lineSeparator());
        builder.append("=================")
                .append(System.lineSeparator());

        builder.append("Chế độ tải: ")
                .append(threadCount)
                .append(threadCount == 1 ? " luồng" : " luồng song song")
                .append(System.lineSeparator());

        builder.append("Số fanpage đã chọn: ")
                .append(rows.size())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        int totalVideoCount = 0;
        Set<String> warningPages = new LinkedHashSet<>();

        for (DownloadPlanGuiRow row : rows) {
            totalVideoCount = totalVideoCount + row.getDownloadCount();

            builder.append(row.getPageCode())
                    .append(" - ")
                    .append(row.getPageName())
                    .append(System.lineSeparator());

            builder.append("  Source: ")
                    .append(emptyToDash(row.getSourceCode()))
                    .append(" - ")
                    .append(emptyToDash(row.getSourceName()))
                    .append(System.lineSeparator());

            builder.append("  Loại: ")
                    .append(emptyToDash(row.getSourceType()))
                    .append(System.lineSeparator());

            builder.append("  Số video cần tải: ")
                    .append(row.getDownloadCount())
                    .append(System.lineSeparator());

            builder.append("  Link: ")
                    .append(emptyToDash(row.getSourceUrl()))
                    .append(System.lineSeparator());

            if (row.getSourceId() == null || row.getSourceUrl() == null || row.getSourceUrl().isBlank()) {
                warningPages.add(row.getPageCode() + " - " + row.getPageName());
                builder.append("  Cảnh báo: Fanpage này chưa có source active.")
                        .append(System.lineSeparator());
            }

            builder.append(System.lineSeparator());
        }

        builder.append("Tổng video dự kiến tải: ")
                .append(totalVideoCount)
                .append(System.lineSeparator());

        if (!warningPages.isEmpty()) {
            builder.append(System.lineSeparator());
            builder.append("CẢNH BÁO")
                    .append(System.lineSeparator());

            for (String page : warningPages) {
                builder.append("- ")
                        .append(page)
                        .append(" chưa có source active.")
                        .append(System.lineSeparator());
            }
        }

        return builder.toString();
    }

    private Long readNullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);

        if (resultSet.wasNull()) {
            return null;
        }

        return value;
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table'
                  AND name = ?
                LIMIT 1
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, tableName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String emptyToDash(String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }

        return text;
    }
}