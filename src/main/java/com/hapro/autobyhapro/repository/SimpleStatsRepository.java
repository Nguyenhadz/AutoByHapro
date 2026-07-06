package com.hapro.autobyhapro.repository;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.PageSimpleStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SimpleStatsRepository {

    public List<PageSimpleStats> findPageSimpleStats() {
        String sql = """
                SELECT
                    f.id AS fanpage_id,
                    f.page_code,
                    f.page_name,
                    COALESCE(s.source_code, '') AS source_code,
                    COALESCE(s.source_name, '') AS source_name,
                    COALESCE(s.source_type, '') AS source_type,
                    COALESCE(MIN(date(v.downloaded_time)), '') AS start_date,
                    COUNT(v.id) AS total_downloaded_count,
                    SUM(CASE WHEN v.status = 'READY_UPLOAD' THEN 1 ELSE 0 END) AS ready_upload_count,
                    SUM(CASE WHEN v.status = 'UPLOADED_MARKED' THEN 1 ELSE 0 END) AS uploaded_marked_count,
                    SUM(CASE WHEN v.status = 'UPLOADED_DELETED' THEN 1 ELSE 0 END) AS uploaded_deleted_count,
                    SUM(CASE WHEN v.status IN ('UPLOADED_MARKED', 'UPLOADED_DELETED') THEN 1 ELSE 0 END) AS uploaded_total_count,
                    COUNT(DISTINCT CASE
                        WHEN v.downloaded_time IS NOT NULL THEN date(v.downloaded_time)
                    END) AS working_days
                FROM fanpages f
                LEFT JOIN sources s
                    ON s.fanpage_id = f.id
                   AND s.active = 1
                LEFT JOIN videos v
                    ON v.fanpage_id = f.id
                WHERE f.active = 1
                GROUP BY
                    f.id,
                    f.page_code,
                    f.page_name,
                    s.source_code,
                    s.source_name,
                    s.source_type
                ORDER BY f.id ASC
                """;

        List<PageSimpleStats> statsList = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                PageSimpleStats stats = new PageSimpleStats(
                        resultSet.getLong("fanpage_id"),
                        resultSet.getString("page_code"),
                        resultSet.getString("page_name"),
                        resultSet.getString("source_code"),
                        resultSet.getString("source_name"),
                        resultSet.getString("source_type"),
                        resultSet.getString("start_date"),
                        resultSet.getInt("total_downloaded_count"),
                        resultSet.getInt("ready_upload_count"),
                        resultSet.getInt("uploaded_marked_count"),
                        resultSet.getInt("uploaded_deleted_count"),
                        resultSet.getInt("uploaded_total_count"),
                        resultSet.getInt("working_days")
                );

                statsList.add(stats);
            }

            return statsList;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy thống kê đơn giản.", exception);
        }
    }
}