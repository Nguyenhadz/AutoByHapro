package com.hapro.autobyhapro.repository;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.EditedVideoTarget;

import java.nio.file.Path;
import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

public class VideoRepository {

    public Set<String> findExistingVideoIds(Long sourceId, Collection<String> videoIds) {
        Set<String> existingVideoIds = new HashSet<>();

        if (sourceId == null || videoIds == null || videoIds.isEmpty()) {
            return existingVideoIds;
        }

        StringJoiner placeholders = new StringJoiner(",");

        for (int i = 0; i < videoIds.size(); i++) {
            placeholders.add("?");
        }

        String sql = """
                SELECT platform_video_id
                FROM videos
                WHERE source_id = ?
                  AND platform_video_id IN (%s)
                """.formatted(placeholders);

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, sourceId);

            int parameterIndex = 2;

            for (String videoId : videoIds) {
                preparedStatement.setString(parameterIndex, videoId);
                parameterIndex++;
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    existingVideoIds.add(resultSet.getString("platform_video_id"));
                }
            }

            return existingVideoIds;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể kiểm tra video đã tồn tại trong database.", exception);
        }
    }

    public long saveDownloadedVideo(
            Long batchId,
            Long sourceId,
            Long fanpageId,
            String platformVideoId,
            String title,
            String channelName,
            String originalUrl,
            String rawFilePath
    ) {
        String insertVideoSql = """
                INSERT INTO videos (
                    batch_id,
                    source_id,
                    fanpage_id,
                    platform_video_id,
                    title,
                    channel_name,
                    original_url,
                    downloaded_time,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'), 'DOWNLOADED')
                """;

        String insertFileSql = """
                INSERT INTO video_files (
                    video_id,
                    file_type,
                    file_path,
                    file_exists
                )
                VALUES (?, 'RAW', ?, 1)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (
                    PreparedStatement videoStatement = connection.prepareStatement(
                            insertVideoSql,
                            Statement.RETURN_GENERATED_KEYS
                    );
                    PreparedStatement fileStatement = connection.prepareStatement(insertFileSql)
            ) {
                videoStatement.setLong(1, batchId);
                videoStatement.setLong(2, sourceId);
                videoStatement.setLong(3, fanpageId);
                videoStatement.setString(4, platformVideoId);
                videoStatement.setString(5, title);
                videoStatement.setString(6, channelName);
                videoStatement.setString(7, originalUrl);

                videoStatement.executeUpdate();

                long videoId;

                try (ResultSet generatedKeys = videoStatement.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new RuntimeException("Không lấy được ID video vừa lưu.");
                    }

                    videoId = generatedKeys.getLong(1);
                }

                fileStatement.setLong(1, videoId);
                fileStatement.setString(2, rawFilePath);
                fileStatement.executeUpdate();

                connection.commit();

                return videoId;

            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }

        } catch (Exception exception) {
            throw new RuntimeException("Không thể lưu video đã tải vào database.", exception);
        }
    }

    public EditedVideoTarget findEditedVideoTarget(String batchCode, String platformVideoIdOrPrefix) {
        String cleanBatchCode = normalizeBatchCode(batchCode);
        String cleanVideoId = cleanText(platformVideoIdOrPrefix);

        if (cleanVideoId.isBlank()) {
            return null;
        }

        if (!cleanBatchCode.isBlank()) {
            EditedVideoTarget exactTarget = findEditedVideoTargetByBatchAndVideoId(
                    cleanBatchCode,
                    cleanVideoId
            );

            if (exactTarget != null) {
                return exactTarget;
            }

            EditedVideoTarget prefixTarget = findEditedVideoTargetByBatchAndVideoIdPrefix(
                    cleanBatchCode,
                    cleanVideoId
            );

            if (prefixTarget != null) {
                return prefixTarget;
            }
        }

        EditedVideoTarget exactTarget = findEditedVideoTargetByVideoId(cleanVideoId);

        if (exactTarget != null) {
            return exactTarget;
        }

        return findEditedVideoTargetByVideoIdPrefix(cleanVideoId);
    }

    public EditedVideoTarget findEditedVideoTargetByFileNameApprox(String editedFileName) {
        String editedKey = normalizeForCompare(editedFileName);

        if (editedKey.length() < 12) {
            return null;
        }

        String sql = """
                SELECT
                    v.id AS video_id,
                    v.batch_id,
                    v.platform_video_id,
                    v.title,
                    v.status,
                    b.batch_code,
                    b.edited_folder_path,
                    raw.raw_file_path
                FROM videos v
                INNER JOIN video_batches b ON b.id = v.batch_id
                INNER JOIN (
                    SELECT
                        video_id,
                        MIN(file_path) AS raw_file_path
                    FROM video_files
                    WHERE file_type = 'RAW'
                    GROUP BY video_id
                ) raw ON raw.video_id = v.id
                ORDER BY v.id DESC
                """;

        EditedVideoTarget bestTarget = null;
        int bestScore = -1;
        boolean ambiguous = false;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                String rawFilePath = resultSet.getString("raw_file_path");

                if (rawFilePath == null || rawFilePath.isBlank()) {
                    continue;
                }

                String rawFileName = Path.of(rawFilePath).getFileName().toString();
                String rawKey = normalizeForCompare(rawFileName);

                int score = comparePrefixScore(rawKey, editedKey);

                if (score <= 0) {
                    continue;
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = mapEditedVideoTarget(resultSet);
                    ambiguous = false;
                } else if (score == bestScore) {
                    ambiguous = true;
                }
            }

            if (ambiguous) {
                return null;
            }

            return bestTarget;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tìm video theo tên gần đúng.", exception);
        }
    }

    private int comparePrefixScore(String rawKey, String editedKey) {
        if (rawKey == null || editedKey == null) {
            return -1;
        }

        if (rawKey.isBlank() || editedKey.isBlank()) {
            return -1;
        }

        if (rawKey.startsWith(editedKey)) {
            return editedKey.length();
        }

        if (editedKey.startsWith(rawKey)) {
            return rawKey.length();
        }

        return -1;
    }

    private EditedVideoTarget findEditedVideoTargetByBatchAndVideoId(
            String batchCode,
            String platformVideoId
    ) {
        String sql = """
                SELECT
                    v.id AS video_id,
                    v.batch_id,
                    v.platform_video_id,
                    v.title,
                    v.status,
                    b.batch_code,
                    b.edited_folder_path,
                    raw.raw_file_path
                FROM videos v
                INNER JOIN video_batches b ON b.id = v.batch_id
                LEFT JOIN (
                    SELECT
                        video_id,
                        MIN(file_path) AS raw_file_path
                    FROM video_files
                    WHERE file_type = 'RAW'
                    GROUP BY video_id
                ) raw ON raw.video_id = v.id
                WHERE b.batch_code = ?
                  AND v.platform_video_id = ?
                LIMIT 1
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, batchCode);
            preparedStatement.setString(2, platformVideoId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapEditedVideoTarget(resultSet);
                }
            }

            return null;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tìm video đã edit theo batch/video_id.", exception);
        }
    }

    private EditedVideoTarget findEditedVideoTargetByBatchAndVideoIdPrefix(
            String batchCode,
            String platformVideoIdPrefix
    ) {
        if (platformVideoIdPrefix.length() < 4) {
            return null;
        }

        String sql = """
                SELECT
                    v.id AS video_id,
                    v.batch_id,
                    v.platform_video_id,
                    v.title,
                    v.status,
                    b.batch_code,
                    b.edited_folder_path,
                    raw.raw_file_path
                FROM videos v
                INNER JOIN video_batches b ON b.id = v.batch_id
                LEFT JOIN (
                    SELECT
                        video_id,
                        MIN(file_path) AS raw_file_path
                    FROM video_files
                    WHERE file_type = 'RAW'
                    GROUP BY video_id
                ) raw ON raw.video_id = v.id
                WHERE b.batch_code = ?
                  AND v.platform_video_id LIKE ?
                ORDER BY v.id DESC
                LIMIT 2
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, batchCode);
            preparedStatement.setString(2, platformVideoIdPrefix + "%");

            return readUniqueTarget(preparedStatement);

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tìm video theo prefix trong batch.", exception);
        }
    }

    private EditedVideoTarget findEditedVideoTargetByVideoId(String platformVideoId) {
        String sql = """
                SELECT
                    v.id AS video_id,
                    v.batch_id,
                    v.platform_video_id,
                    v.title,
                    v.status,
                    b.batch_code,
                    b.edited_folder_path,
                    raw.raw_file_path
                FROM videos v
                INNER JOIN video_batches b ON b.id = v.batch_id
                LEFT JOIN (
                    SELECT
                        video_id,
                        MIN(file_path) AS raw_file_path
                    FROM video_files
                    WHERE file_type = 'RAW'
                    GROUP BY video_id
                ) raw ON raw.video_id = v.id
                WHERE v.platform_video_id = ?
                ORDER BY v.id DESC
                LIMIT 1
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, platformVideoId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapEditedVideoTarget(resultSet);
                }
            }

            return null;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tìm video đã edit theo video_id.", exception);
        }
    }

    private EditedVideoTarget findEditedVideoTargetByVideoIdPrefix(String platformVideoIdPrefix) {
        if (platformVideoIdPrefix.length() < 6) {
            return null;
        }

        String sql = """
                SELECT
                    v.id AS video_id,
                    v.batch_id,
                    v.platform_video_id,
                    v.title,
                    v.status,
                    b.batch_code,
                    b.edited_folder_path,
                    raw.raw_file_path
                FROM videos v
                INNER JOIN video_batches b ON b.id = v.batch_id
                LEFT JOIN (
                    SELECT
                        video_id,
                        MIN(file_path) AS raw_file_path
                    FROM video_files
                    WHERE file_type = 'RAW'
                    GROUP BY video_id
                ) raw ON raw.video_id = v.id
                WHERE v.platform_video_id LIKE ?
                ORDER BY v.id DESC
                LIMIT 2
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, platformVideoIdPrefix + "%");

            return readUniqueTarget(preparedStatement);

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tìm video theo prefix.", exception);
        }
    }

    private EditedVideoTarget readUniqueTarget(PreparedStatement preparedStatement) throws SQLException {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            EditedVideoTarget first = null;
            int count = 0;

            while (resultSet.next()) {
                count++;

                if (count == 1) {
                    first = mapEditedVideoTarget(resultSet);
                }

                if (count > 1) {
                    return null;
                }
            }

            return first;
        }
    }

    private EditedVideoTarget mapEditedVideoTarget(ResultSet resultSet) throws SQLException {
        return new EditedVideoTarget(
                resultSet.getLong("video_id"),
                resultSet.getLong("batch_id"),
                resultSet.getString("platform_video_id"),
                resultSet.getString("title"),
                resultSet.getString("status"),
                resultSet.getString("batch_code"),
                resultSet.getString("edited_folder_path"),
                resultSet.getString("raw_file_path")
        );
    }

    public void saveEditedVideoFile(
            Long videoId,
            String editedFilePath
    ) {
        String insertFileSql = """
                INSERT INTO video_files (
                    video_id,
                    file_type,
                    file_path,
                    file_exists
                )
                VALUES (?, 'EDITED', ?, 1)
                """;

        String updateVideoSql = """
                UPDATE videos
                SET status = 'READY_UPLOAD'
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (
                    PreparedStatement insertFileStatement = connection.prepareStatement(insertFileSql);
                    PreparedStatement updateVideoStatement = connection.prepareStatement(updateVideoSql)
            ) {
                insertFileStatement.setLong(1, videoId);
                insertFileStatement.setString(2, editedFilePath);
                insertFileStatement.executeUpdate();

                updateVideoStatement.setLong(1, videoId);
                updateVideoStatement.executeUpdate();

                connection.commit();

            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }

        } catch (Exception exception) {
            throw new RuntimeException("Không thể lưu file edit vào database.", exception);
        }
    }

    public void updateVideoBatchStatus(Long videoBatchId, String status) {
        String sql = """
                UPDATE video_batches
                SET status = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, status);
            preparedStatement.setLong(2, videoBatchId);
            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể cập nhật trạng thái video batch.", exception);
        }
    }

    private String normalizeBatchCode(String batchCode) {
        if (batchCode == null || batchCode.isBlank()) {
            return "";
        }

        String clean = batchCode.trim();

        if (clean.matches("P\\d{3}__D\\d{6}__B\\d{3}")) {
            return clean;
        }

        if (clean.matches("P\\d{3}_D\\d{6}_B\\d{3}")) {
            return clean.replace("_D", "__D")
                    .replace("_B", "__B");
        }

        if (clean.matches("P\\d{3}D\\d{6}B\\d{3}")) {
            return clean.substring(0, 4)
                    + "__"
                    + clean.substring(4, 11)
                    + "__"
                    + clean.substring(11);
        }

        return clean;
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }

    private String normalizeForCompare(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        String name = fileName.trim();

        int dotIndex = name.lastIndexOf(".");

        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }

        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }
}