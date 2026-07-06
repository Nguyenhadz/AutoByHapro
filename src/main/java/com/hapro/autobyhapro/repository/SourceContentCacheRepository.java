package com.hapro.autobyhapro.repository;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.VideoCandidate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SourceContentCacheRepository {

    public String findStableContentType(Long sourceId, String platformPostId) {
        if (sourceId == null || platformPostId == null || platformPostId.isBlank()) {
            return "";
        }

        String sql = """
                SELECT content_type
                FROM source_content_cache
                WHERE source_id = ?
                  AND platform_post_id = ?
                  AND content_type IN ('VIDEO', 'PHOTO', 'AUDIO_ONLY')
                LIMIT 1
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, sourceId);
            preparedStatement.setString(2, platformPostId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("content_type");
                }

                return "";
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể đọc cache loại bài đăng.", exception);
        }
    }

    public void saveCheckResult(
            Long sourceId,
            String sourceType,
            VideoCandidate candidate,
            String contentType,
            String checkStatus,
            String message
    ) {
        if (sourceId == null || candidate == null) {
            return;
        }

        String platformPostId = candidate.getVideoId();

        if (platformPostId == null || platformPostId.isBlank()) {
            return;
        }

        String safeSourceType = sourceType == null ? "" : sourceType;
        String safeContentType = normalizeContentType(contentType);
        String safeCheckStatus = normalizeCheckStatus(checkStatus);
        String safeMessage = shorten(message, 1000);

        String sql = """
                INSERT INTO source_content_cache (
                    source_id,
                    source_type,
                    platform_post_id,
                    url,
                    title,
                    content_type,
                    check_status,
                    checked_time,
                    last_seen_time,
                    message
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'), ?)
                ON CONFLICT(source_id, platform_post_id)
                DO UPDATE SET
                    source_type = excluded.source_type,
                    url = excluded.url,
                    title = excluded.title,
                    content_type = excluded.content_type,
                    check_status = excluded.check_status,
                    checked_time = datetime('now'),
                    last_seen_time = datetime('now'),
                    message = excluded.message
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, sourceId);
            preparedStatement.setString(2, safeSourceType);
            preparedStatement.setString(3, platformPostId);
            preparedStatement.setString(4, candidate.getUrl());
            preparedStatement.setString(5, candidate.getTitle());
            preparedStatement.setString(6, safeContentType);
            preparedStatement.setString(7, safeCheckStatus);
            preparedStatement.setString(8, safeMessage);

            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lưu cache loại bài đăng.", exception);
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "UNKNOWN";
        }

        String upper = contentType.trim().toUpperCase();

        return switch (upper) {
            case "VIDEO", "PHOTO", "AUDIO_ONLY", "UNKNOWN", "ERROR" -> upper;
            default -> "UNKNOWN";
        };
    }

    private String normalizeCheckStatus(String checkStatus) {
        if (checkStatus == null || checkStatus.isBlank()) {
            return "OK";
        }

        String upper = checkStatus.trim().toUpperCase();

        return switch (upper) {
            case "OK", "ERROR" -> upper;
            default -> "OK";
        };
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}