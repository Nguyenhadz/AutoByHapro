package com.hapro.autobyhapro.repository;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.VideoFileCheckItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileSyncRepository {

    public List<VideoFileCheckItem> findAllVideoFiles() {
        String sql = """
                SELECT
                    id,
                    video_id,
                    file_type,
                    file_path,
                    file_exists
                FROM video_files
                ORDER BY id ASC
                """;

        List<VideoFileCheckItem> items = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                VideoFileCheckItem item = new VideoFileCheckItem(
                        resultSet.getLong("id"),
                        resultSet.getLong("video_id"),
                        resultSet.getString("file_type"),
                        resultSet.getString("file_path"),
                        resultSet.getInt("file_exists") == 1,
                        false
                );

                items.add(item);
            }

            return items;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách video_files.", exception);
        }
    }

    public void updateVideoFileExists(Long videoFileId, boolean fileExists) {
        String sql = """
                UPDATE video_files
                SET file_exists = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, fileExists ? 1 : 0);
            preparedStatement.setLong(2, videoFileId);
            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể cập nhật file_exists.", exception);
        }
    }

    public int updateReadyUploadMissingEditedBackToDownloaded() {
        String sql = """
                UPDATE videos
                SET status = 'DOWNLOADED'
                WHERE status = 'READY_UPLOAD'
                  AND EXISTS (
                      SELECT 1
                      FROM video_files vf
                      WHERE vf.video_id = videos.id
                        AND vf.file_type = 'RAW'
                        AND vf.file_exists = 1
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM video_files vf
                      WHERE vf.video_id = videos.id
                        AND vf.file_type = 'EDITED'
                        AND vf.file_exists = 1
                  )
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            return preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể đổi READY_UPLOAD về DOWNLOADED.", exception);
        }
    }

    public int updateActiveVideosMissingAllFiles() {
        String sql = """
                UPDATE videos
                SET status = 'FILE_MISSING'
                WHERE status IN ('DOWNLOADED', 'READY_UPLOAD')
                  AND NOT EXISTS (
                      SELECT 1
                      FROM video_files vf
                      WHERE vf.video_id = videos.id
                        AND vf.file_exists = 1
                  )
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            return preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể đổi video mất file sang FILE_MISSING.", exception);
        }
    }

    public int refreshVideoBatchStatuses() {
        List<Long> batchIds = findAllVideoBatchIds();
        int updatedCount = 0;

        for (Long batchId : batchIds) {
            String oldStatus = findVideoBatchStatus(batchId);
            String newStatus = calculateVideoBatchStatus(batchId);

            if (newStatus == null || newStatus.isBlank()) {
                continue;
            }

            if (!newStatus.equals(oldStatus)) {
                updateVideoBatchStatus(batchId, newStatus);
                updatedCount++;
            }
        }

        return updatedCount;
    }

    private List<Long> findAllVideoBatchIds() {
        String sql = """
                SELECT id
                FROM video_batches
                ORDER BY id ASC
                """;

        List<Long> ids = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                ids.add(resultSet.getLong("id"));
            }

            return ids;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách video batch.", exception);
        }
    }

    private String findVideoBatchStatus(Long batchId) {
        String sql = """
                SELECT status
                FROM video_batches
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, batchId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("status");
                }

                return "";
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy trạng thái batch.", exception);
        }
    }

    private String calculateVideoBatchStatus(Long batchId) {
        String sql = """
                SELECT
                    COUNT(id) AS total_count,
                    SUM(CASE WHEN status = 'DOWNLOADED' THEN 1 ELSE 0 END) AS downloaded_count,
                    SUM(CASE WHEN status = 'READY_UPLOAD' THEN 1 ELSE 0 END) AS ready_upload_count,
                    SUM(CASE WHEN status = 'UPLOADED_MARKED' THEN 1 ELSE 0 END) AS uploaded_marked_count,
                    SUM(CASE WHEN status = 'UPLOADED_DELETED' THEN 1 ELSE 0 END) AS uploaded_deleted_count,
                    SUM(CASE WHEN status = 'FILE_MISSING' THEN 1 ELSE 0 END) AS file_missing_count
                FROM videos
                WHERE batch_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, batchId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return "EMPTY";
                }

                int totalCount = resultSet.getInt("total_count");
                int downloadedCount = resultSet.getInt("downloaded_count");
                int readyUploadCount = resultSet.getInt("ready_upload_count");
                int uploadedMarkedCount = resultSet.getInt("uploaded_marked_count");
                int uploadedDeletedCount = resultSet.getInt("uploaded_deleted_count");
                int fileMissingCount = resultSet.getInt("file_missing_count");

                if (totalCount <= 0) {
                    return "EMPTY";
                }

                if (uploadedDeletedCount >= totalCount) {
                    return "UPLOADED_DELETED";
                }

                if (uploadedMarkedCount >= totalCount) {
                    return "UPLOADED_MARKED";
                }

                if (uploadedMarkedCount > 0) {
                    return "PARTIAL_UPLOADED";
                }

                if (readyUploadCount > 0) {
                    return "READY_UPLOAD";
                }

                if (downloadedCount > 0) {
                    return "DOWNLOADED";
                }

                if (fileMissingCount >= totalCount) {
                    return "FILE_MISSING";
                }

                return "MIXED";
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tính trạng thái batch.", exception);
        }
    }

    private void updateVideoBatchStatus(Long batchId, String status) {
        String sql = """
                UPDATE video_batches
                SET status = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, status);
            preparedStatement.setLong(2, batchId);
            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể cập nhật trạng thái batch.", exception);
        }
    }
}
