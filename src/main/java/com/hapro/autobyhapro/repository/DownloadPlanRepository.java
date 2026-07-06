package com.hapro.autobyhapro.repository;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.DownloadTarget;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DownloadPlanRepository {

    public List<DownloadTarget> findActiveDownloadTargets() {
        String sql = """
                SELECT
                    f.id AS fanpage_id,
                    f.page_code,
                    f.page_name,
                    f.default_video_count,
                    s.id AS source_id,
                    s.source_code,
                    s.source_name,
                    s.source_type,
                    s.source_url
                FROM fanpages f
                INNER JOIN sources s
                    ON s.fanpage_id = f.id
                   AND s.active = 1
                WHERE f.active = 1
                ORDER BY f.id ASC
                """;

        List<DownloadTarget> targets = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                DownloadTarget target = new DownloadTarget(
                        resultSet.getLong("fanpage_id"),
                        resultSet.getString("page_code"),
                        resultSet.getString("page_name"),
                        resultSet.getInt("default_video_count"),
                        resultSet.getLong("source_id"),
                        resultSet.getString("source_code"),
                        resultSet.getString("source_name"),
                        resultSet.getString("source_type"),
                        resultSet.getString("source_url")
                );

                targets.add(target);
            }

            return targets;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách fanpage/source để tải.", exception);
        }
    }

    public long createDownloadBatch(
            Long fanpageId,
            Long sourceId,
            int requestedCount,
            int threadCount
    ) {
        String sql = """
                INSERT INTO download_batches (
                    fanpage_id,
                    source_id,
                    requested_count,
                    downloaded_count,
                    thread_count,
                    status
                )
                VALUES (?, ?, ?, 0, ?, 'RUNNING')
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            preparedStatement.setLong(1, fanpageId);
            preparedStatement.setLong(2, sourceId);
            preparedStatement.setInt(3, requestedCount);
            preparedStatement.setInt(4, threadCount);

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }

            throw new RuntimeException("Không lấy được ID download batch vừa tạo.");

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tạo download batch.", exception);
        }
    }

    public long createVideoBatch(
            String batchCode,
            Long fanpageId,
            Long sourceId,
            Long downloadBatchId,
            int batchIndex,
            int videoCount,
            String rawFolderPath,
            String editedFolderPath
    ) {
        String sql = """
                INSERT INTO video_batches (
                    batch_code,
                    fanpage_id,
                    source_id,
                    download_batch_id,
                    batch_index,
                    video_count,
                    raw_folder_path,
                    edited_folder_path,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'RUNNING')
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            preparedStatement.setString(1, batchCode);
            preparedStatement.setLong(2, fanpageId);
            preparedStatement.setLong(3, sourceId);
            preparedStatement.setLong(4, downloadBatchId);
            preparedStatement.setInt(5, batchIndex);
            preparedStatement.setInt(6, videoCount);
            preparedStatement.setString(7, rawFolderPath);
            preparedStatement.setString(8, editedFolderPath);

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }

            throw new RuntimeException("Không lấy được ID video batch vừa tạo.");

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tạo video batch.", exception);
        }
    }

    public void updateDownloadBatchResult(
            Long downloadBatchId,
            int downloadedCount,
            String status
    ) {
        String sql = """
                UPDATE download_batches
                SET downloaded_count = ?,
                    status = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, downloadedCount);
            preparedStatement.setString(2, status);
            preparedStatement.setLong(3, downloadBatchId);
            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể cập nhật download batch.", exception);
        }
    }

    public void updateVideoBatchResult(
            Long videoBatchId,
            int actualDownloadedCount,
            String status
    ) {
        String sql = """
                UPDATE video_batches
                SET video_count = ?,
                    status = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, actualDownloadedCount);
            preparedStatement.setString(2, status);
            preparedStatement.setLong(3, videoBatchId);
            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể cập nhật video batch.", exception);
        }
    }
}