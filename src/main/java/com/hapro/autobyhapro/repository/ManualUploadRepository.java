package com.hapro.autobyhapro.repository;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.CleanupDatabaseResult;
import com.hapro.autobyhapro.entity.ManualUploadMarkResult;
import com.hapro.autobyhapro.entity.ReadyUploadBatch;
import com.hapro.autobyhapro.entity.UploadedMarkedBatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ManualUploadRepository {

    public List<ReadyUploadBatch> findReadyUploadBatches() {
        String sql = """
                SELECT
                    b.id AS video_batch_id,
                    b.batch_code,
                    b.edited_folder_path,
                    b.status AS batch_status,
                    f.page_code,
                    f.page_name,
                    COUNT(v.id) AS total_video_count,
                    SUM(CASE WHEN v.status = 'READY_UPLOAD' THEN 1 ELSE 0 END) AS ready_video_count
                FROM video_batches b
                INNER JOIN fanpages f ON f.id = b.fanpage_id
                INNER JOIN videos v ON v.batch_id = b.id
                GROUP BY
                    b.id,
                    b.batch_code,
                    b.edited_folder_path,
                    b.status,
                    f.page_code,
                    f.page_name
                HAVING ready_video_count > 0
                ORDER BY b.id ASC
                """;

        List<ReadyUploadBatch> batches = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                ReadyUploadBatch batch = new ReadyUploadBatch(
                        resultSet.getLong("video_batch_id"),
                        resultSet.getString("batch_code"),
                        resultSet.getString("page_code"),
                        resultSet.getString("page_name"),
                        resultSet.getString("edited_folder_path"),
                        resultSet.getString("batch_status"),
                        resultSet.getInt("ready_video_count"),
                        resultSet.getInt("total_video_count")
                );

                batches.add(batch);
            }

            return batches;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách folder sẵn sàng upload.", exception);
        }
    }

    public List<UploadedMarkedBatch> findUploadedMarkedBatches() {
        String sql = """
                SELECT
                    b.id AS video_batch_id,
                    b.batch_code,
                    b.raw_folder_path,
                    b.edited_folder_path,
                    b.status AS batch_status,
                    f.page_code,
                    f.page_name,
                    COUNT(v.id) AS total_video_count,
                    SUM(CASE WHEN v.status = 'UPLOADED_MARKED' THEN 1 ELSE 0 END) AS uploaded_marked_video_count
                FROM video_batches b
                INNER JOIN fanpages f ON f.id = b.fanpage_id
                INNER JOIN videos v ON v.batch_id = b.id
                GROUP BY
                    b.id,
                    b.batch_code,
                    b.raw_folder_path,
                    b.edited_folder_path,
                    b.status,
                    f.page_code,
                    f.page_name
                HAVING uploaded_marked_video_count > 0
                ORDER BY b.id ASC
                """;

        List<UploadedMarkedBatch> batches = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                UploadedMarkedBatch batch = new UploadedMarkedBatch(
                        resultSet.getLong("video_batch_id"),
                        resultSet.getString("batch_code"),
                        resultSet.getString("page_code"),
                        resultSet.getString("page_name"),
                        resultSet.getString("raw_folder_path"),
                        resultSet.getString("edited_folder_path"),
                        resultSet.getString("batch_status"),
                        resultSet.getInt("uploaded_marked_video_count"),
                        resultSet.getInt("total_video_count")
                );

                batches.add(batch);
            }

            return batches;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách folder đã upload chờ dọn.", exception);
        }
    }

    public ManualUploadMarkResult markBatchUploaded(ReadyUploadBatch batch) {
        if (batch == null) {
            throw new RuntimeException("Batch upload đang bị trống.");
        }

        String updateVideosSql = """
                UPDATE videos
                SET status = 'UPLOADED_MARKED'
                WHERE batch_id = ?
                  AND status = 'READY_UPLOAD'
                """;

        String updateBatchSql = """
                UPDATE video_batches
                SET status = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (
                    PreparedStatement updateVideosStatement = connection.prepareStatement(updateVideosSql);
                    PreparedStatement updateBatchStatement = connection.prepareStatement(updateBatchSql)
            ) {
                updateVideosStatement.setLong(1, batch.getVideoBatchId());
                int markedVideoCount = updateVideosStatement.executeUpdate();

                String newBatchStatus = calculateUploadBatchStatus(
                        connection,
                        batch.getVideoBatchId()
                );

                updateBatchStatement.setString(1, newBatchStatus);
                updateBatchStatement.setLong(2, batch.getVideoBatchId());
                updateBatchStatement.executeUpdate();

                connection.commit();

                return new ManualUploadMarkResult(
                        batch.getVideoBatchId(),
                        batch.getBatchCode(),
                        markedVideoCount,
                        newBatchStatus,
                        "Đã đánh dấu upload thành công."
                );

            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }

        } catch (Exception exception) {
            throw new RuntimeException("Không thể đánh dấu folder đã upload.", exception);
        }
    }

    public List<String> findUploadedMarkedFilePaths(Long videoBatchId) {
        if (videoBatchId == null) {
            throw new RuntimeException("Video batch ID đang bị trống.");
        }

        String sql = """
                SELECT vf.file_path
                FROM video_files vf
                INNER JOIN videos v ON v.id = vf.video_id
                WHERE v.batch_id = ?
                  AND v.status = 'UPLOADED_MARKED'
                  AND vf.file_path IS NOT NULL
                  AND TRIM(vf.file_path) <> ''
                ORDER BY vf.id ASC
                """;

        List<String> filePaths = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, videoBatchId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    filePaths.add(resultSet.getString("file_path"));
                }
            }

            return filePaths;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách file cần dọn sau upload.", exception);
        }
    }

    public CleanupDatabaseResult cleanupUploadedBatchInDatabase(Long videoBatchId) {
        if (videoBatchId == null) {
            throw new RuntimeException("Video batch ID đang bị trống.");
        }

        String deleteVideoFilesSql = """
                DELETE FROM video_files
                WHERE video_id IN (
                    SELECT id
                    FROM videos
                    WHERE batch_id = ?
                      AND status = 'UPLOADED_MARKED'
                )
                """;

        String updateVideosSql = """
                UPDATE videos
                SET status = 'UPLOADED_DELETED'
                WHERE batch_id = ?
                  AND status = 'UPLOADED_MARKED'
                """;

        String updateBatchSql = """
                UPDATE video_batches
                SET status = ?
                WHERE id = ?
                """;

        String updateDownloadBatchSql = """
                UPDATE download_batches
                SET status = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (
                    PreparedStatement deleteVideoFilesStatement = connection.prepareStatement(deleteVideoFilesSql);
                    PreparedStatement updateVideosStatement = connection.prepareStatement(updateVideosSql);
                    PreparedStatement updateBatchStatement = connection.prepareStatement(updateBatchSql);
                    PreparedStatement updateDownloadBatchStatement = connection.prepareStatement(updateDownloadBatchSql)
            ) {
                Long downloadBatchId = findDownloadBatchId(connection, videoBatchId);

                deleteVideoFilesStatement.setLong(1, videoBatchId);
                int deletedVideoFilesCount = deleteVideoFilesStatement.executeUpdate();

                updateVideosStatement.setLong(1, videoBatchId);
                int updatedVideosCount = updateVideosStatement.executeUpdate();

                String newBatchStatus = calculateCleanupBatchStatus(connection, videoBatchId);

                updateBatchStatement.setString(1, newBatchStatus);
                updateBatchStatement.setLong(2, videoBatchId);
                updateBatchStatement.executeUpdate();

                String newDownloadBatchStatus = "";

                if (downloadBatchId != null) {
                    newDownloadBatchStatus = calculateDownloadBatchCleanupStatus(
                            connection,
                            downloadBatchId
                    );

                    updateDownloadBatchStatement.setString(1, newDownloadBatchStatus);
                    updateDownloadBatchStatement.setLong(2, downloadBatchId);
                    updateDownloadBatchStatement.executeUpdate();
                }

                connection.commit();

                return new CleanupDatabaseResult(
                        deletedVideoFilesCount,
                        updatedVideosCount,
                        newBatchStatus,
                        newDownloadBatchStatus
                );

            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }

        } catch (Exception exception) {
            throw new RuntimeException("Không thể dọn database sau upload.", exception);
        }
    }

    private String calculateUploadBatchStatus(
            Connection connection,
            Long videoBatchId
    ) throws SQLException {
        String sql = """
                SELECT
                    COUNT(id) AS total_count,
                    SUM(CASE WHEN status = 'UPLOADED_MARKED' THEN 1 ELSE 0 END) AS uploaded_count
                FROM videos
                WHERE batch_id = ?
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, videoBatchId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return "UPLOADED_MARKED";
                }

                int totalCount = resultSet.getInt("total_count");
                int uploadedCount = resultSet.getInt("uploaded_count");

                if (totalCount <= 0) {
                    return "UPLOADED_MARKED";
                }

                if (uploadedCount >= totalCount) {
                    return "UPLOADED_MARKED";
                }

                if (uploadedCount > 0) {
                    return "PARTIAL_UPLOADED";
                }

                return "READY_UPLOAD";
            }
        }
    }

    private String calculateCleanupBatchStatus(
            Connection connection,
            Long videoBatchId
    ) throws SQLException {
        String sql = """
                SELECT
                    COUNT(id) AS total_count,
                    SUM(CASE WHEN status = 'UPLOADED_DELETED' THEN 1 ELSE 0 END) AS deleted_count
                FROM videos
                WHERE batch_id = ?
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, videoBatchId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return "UPLOADED_DELETED";
                }

                int totalCount = resultSet.getInt("total_count");
                int deletedCount = resultSet.getInt("deleted_count");

                if (totalCount <= 0) {
                    return "UPLOADED_DELETED";
                }

                if (deletedCount >= totalCount) {
                    return "UPLOADED_DELETED";
                }

                if (deletedCount > 0) {
                    return "PARTIAL_UPLOADED_DELETED";
                }

                return "UPLOADED_MARKED";
            }
        }
    }

    private Long findDownloadBatchId(
            Connection connection,
            Long videoBatchId
    ) throws SQLException {
        String sql = """
                SELECT download_batch_id
                FROM video_batches
                WHERE id = ?
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, videoBatchId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    long value = resultSet.getLong("download_batch_id");

                    if (resultSet.wasNull()) {
                        return null;
                    }

                    return value;
                }

                return null;
            }
        }
    }

    private String calculateDownloadBatchCleanupStatus(
            Connection connection,
            Long downloadBatchId
    ) throws SQLException {
        String sql = """
                SELECT
                    COUNT(id) AS total_count,
                    SUM(CASE WHEN status = 'UPLOADED_DELETED' THEN 1 ELSE 0 END) AS deleted_count,
                    SUM(CASE WHEN status = 'PARTIAL_UPLOADED_DELETED' THEN 1 ELSE 0 END) AS partial_deleted_count
                FROM video_batches
                WHERE download_batch_id = ?
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, downloadBatchId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return "UPLOADED_DELETED";
                }

                int totalCount = resultSet.getInt("total_count");
                int deletedCount = resultSet.getInt("deleted_count");
                int partialDeletedCount = resultSet.getInt("partial_deleted_count");

                if (totalCount <= 0) {
                    return "UPLOADED_DELETED";
                }

                if (deletedCount >= totalCount) {
                    return "UPLOADED_DELETED";
                }

                if (deletedCount > 0 || partialDeletedCount > 0) {
                    return "PARTIAL_UPLOADED_DELETED";
                }

                return "PARTIAL";
            }
        }
    }
}
