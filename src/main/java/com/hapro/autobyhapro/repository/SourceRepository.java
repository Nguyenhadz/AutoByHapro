package com.hapro.autobyhapro.repository;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.Source;

import java.sql.*;
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

    public void saveNewActiveSource(Source source) {
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
                    PreparedStatement insertStatement = connection.prepareStatement(insertNewSourceSql)
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

                connection.commit();

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lưu source.", exception);
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
                    s.created_time
                FROM sources s
                INNER JOIN fanpages f ON f.id = s.fanpage_id
                ORDER BY s.id DESC
                """;

        List<Source> sources = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Source source = new Source(
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
                        resultSet.getString("created_time")
                );

                sources.add(source);
            }

            return sources;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách source.", exception);
        }
    }
}