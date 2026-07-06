package com.hapro.autobyhapro.repository;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.VideoExportRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CsvExportRepository {

    public List<VideoExportRow> findAllVideosForExport() {
        String sql = """
                SELECT
                    v.id AS video_id,
                    f.page_code,
                    f.page_name,
                    COALESCE(s.source_code, '') AS source_code,
                    COALESCE(s.source_name, '') AS source_name,
                    COALESCE(s.source_type, '') AS source_type,
                    v.platform_video_id,
                    COALESCE(v.title, '') AS title,
                    COALESCE(v.status, '') AS status,
                    COALESCE(v.downloaded_time, '') AS downloaded_time,
                    COALESCE(v.original_url, '') AS original_url
                FROM videos v
                INNER JOIN fanpages f ON f.id = v.fanpage_id
                LEFT JOIN sources s ON s.id = v.source_id
                ORDER BY v.id ASC
                """;

        List<VideoExportRow> rows = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                VideoExportRow row = new VideoExportRow(
                        resultSet.getLong("video_id"),
                        resultSet.getString("page_code"),
                        resultSet.getString("page_name"),
                        resultSet.getString("source_code"),
                        resultSet.getString("source_name"),
                        resultSet.getString("source_type"),
                        resultSet.getString("platform_video_id"),
                        resultSet.getString("title"),
                        resultSet.getString("status"),
                        resultSet.getString("downloaded_time"),
                        resultSet.getString("original_url")
                );

                rows.add(row);
            }

            return rows;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách video để xuất CSV.", exception);
        }
    }
}