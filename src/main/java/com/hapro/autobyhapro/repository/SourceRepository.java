package com.hapro.autobyhapro.repository;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.Source;
import com.hapro.autobyhapro.service.TikTokChannelResolverService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SourceRepository {

    public String generateNextSourceCode() {
        String sql = "SELECT MAX(id) AS max_id FROM sources";

        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            long nextId = 1;

            if (resultSet.next()) {
                nextId = resultSet.getLong("max_id") + 1;
            }

            return String.format("S%03d", nextId);

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tạo mã source tiếp theo.", exception);
        }
    }

    public Long saveNewActiveSource(Source source) {
        String deactivateOldSourceSql = """
                UPDATE sources
                SET active = 0
                WHERE fanpage_id = ?
                  AND active = 1
                """;

        String insertNewSourceSql = """
                INSERT INTO sources (
                    fanpage_id,
                    source_code,
                    source_name,
                    source_type,
                    source_url,
                    channel_name,
                    active
                )
                VALUES (?, ?, ?, ?, ?, ?, 1)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (
                    PreparedStatement deactivateStatement = connection.prepareStatement(deactivateOldSourceSql);
                    PreparedStatement insertStatement = connection.prepareStatement(insertNewSourceSql, Statement.RETURN_GENERATED_KEYS)
            ) {
                deactivateStatement.setLong(1, source.getFanpageId());
                deactivateStatement.executeUpdate();

                insertStatement.setLong(1, source.getFanpageId());
                insertStatement.setString(2, source.getSourceCode());
                insertStatement.setString(3, source.getSourceName());
                insertStatement.setString(4, source.getSourceType());
                insertStatement.setString(5, source.getSourceUrl());
                insertStatement.setString(6, source.getChannelName());

                insertStatement.executeUpdate();

                Long newSourceId = null;

                try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newSourceId = generatedKeys.getLong(1);
                    }
                }

                connection.commit();

                autoResolveTikTokSourceIfPossible(
                        newSourceId,
                        source.getSourceType(),
                        source.getSourceUrl()
                );

                return newSourceId;

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lưu source.", exception);
        }
    }


    private void autoResolveTikTokSourceIfPossible(
            Long sourceId,
            String sourceType,
            String sourceUrl
    ) {
        if (sourceId == null) {
            return;
        }

        if (sourceType == null || !sourceType.equalsIgnoreCase("TIKTOK")) {
            return;
        }

        try {
            new TikTokChannelResolverService().resolveSourceUrlForUse(
                    sourceId,
                    sourceType,
                    sourceUrl
            );
        } catch (Exception exception) {
            updateTikTokResolveError(
                    sourceId,
                    exception.getMessage()
            );
        }
    }

    public List<Source> findAll() {
        String sql = """
                SELECT
                    s.id,
                    s.fanpage_id,
                    f.page_code,
                    f.page_name,
                    s.source_code,
                    s.source_name,
                    s.source_type,
                    s.source_url,
                    s.channel_name,
                    s.active,
                    s.created_time,
                    s.tiktok_channel_id,
                    s.resolved_source_url,
                    s.resolved_time,
                    s.resolved_status,
                    s.resolve_message
                FROM sources s
                INNER JOIN fanpages f ON f.id = s.fanpage_id
                ORDER BY s.id DESC
                """;

        List<Source> sources = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Source source = mapSource(resultSet);
                sources.add(source);
            }

            return sources;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách source.", exception);
        }
    }

    public Source findById(Long sourceId) {
        if (sourceId == null) {
            return null;
        }

        String sql = """
                SELECT
                    s.id,
                    s.fanpage_id,
                    f.page_code,
                    f.page_name,
                    s.source_code,
                    s.source_name,
                    s.source_type,
                    s.source_url,
                    s.channel_name,
                    s.active,
                    s.created_time,
                    s.tiktok_channel_id,
                    s.resolved_source_url,
                    s.resolved_time,
                    s.resolved_status,
                    s.resolve_message
                FROM sources s
                LEFT JOIN fanpages f ON f.id = s.fanpage_id
                WHERE s.id = ?
                LIMIT 1
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, sourceId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapSource(resultSet);
                }
            }

            return null;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy source theo ID.", exception);
        }
    }

    public String findResolvedSourceUrlById(Long sourceId) {
        if (sourceId == null) {
            return "";
        }

        String sql = """
                SELECT resolved_source_url
                FROM sources
                WHERE id = ?
                LIMIT 1
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, sourceId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String resolvedSourceUrl = resultSet.getString("resolved_source_url");

                    if (resolvedSourceUrl != null) {
                        return resolvedSourceUrl.trim();
                    }
                }
            }

            return "";

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể đọc resolved_source_url của source.", exception);
        }
    }

    public void updateTikTokResolvedInfo(
            Long sourceId,
            String tiktokChannelId,
            String resolvedSourceUrl,
            String message
    ) {
        if (sourceId == null) {
            return;
        }

        String sql = """
                UPDATE sources
                SET tiktok_channel_id = ?,
                    resolved_source_url = ?,
                    resolved_time = datetime('now'),
                    resolved_status = 'OK',
                    resolve_message = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safeTrim(tiktokChannelId));
            preparedStatement.setString(2, safeTrim(resolvedSourceUrl));
            preparedStatement.setString(3, safeLimit(message, 800));
            preparedStatement.setLong(4, sourceId);

            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lưu TikTok channel_id vào source.", exception);
        }
    }

    public void updateTikTokResolveError(Long sourceId, String message) {
        if (sourceId == null) {
            return;
        }

        String sql = """
                UPDATE sources
                SET resolved_time = datetime('now'),
                    resolved_status = 'ERROR',
                    resolve_message = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, safeLimit(message, 800));
            preparedStatement.setLong(2, sourceId);

            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lưu lỗi resolve TikTok source.", exception);
        }
    }

    private Source mapSource(ResultSet resultSet) throws SQLException {
        return new Source(
                resultSet.getLong("id"),
                resultSet.getLong("fanpage_id"),
                resultSet.getString("page_code"),
                resultSet.getString("page_name"),
                resultSet.getString("source_code"),
                resultSet.getString("source_name"),
                resultSet.getString("source_type"),
                resultSet.getString("source_url"),
                resultSet.getString("channel_name"),
                resultSet.getInt("active") == 1,
                resultSet.getString("created_time"),
                resultSet.getString("tiktok_channel_id"),
                resultSet.getString("resolved_source_url"),
                resultSet.getString("resolved_time"),
                resultSet.getString("resolved_status"),
                resultSet.getString("resolve_message")
        );
    }

    private String safeTrim(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }

    private String safeLimit(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        String cleanText = text.trim();

        if (cleanText.length() <= maxLength) {
            return cleanText;
        }

        return cleanText.substring(0, maxLength);
    }
}
