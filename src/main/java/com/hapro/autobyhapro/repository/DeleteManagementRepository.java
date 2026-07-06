package com.hapro.autobyhapro.repository;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.DeletedSourceInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class DeleteManagementRepository {

    public void ensureDeletedSourcesTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS deleted_sources (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    old_source_id INTEGER,
                    old_source_code TEXT,
                    fanpage_id INTEGER,
                    page_code TEXT,
                    page_name TEXT,
                    source_name TEXT,
                    channel_name TEXT,
                    source_url TEXT,
                    source_type TEXT,
                    deleted_time TEXT DEFAULT (datetime('now'))
                )
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tạo bảng deleted_sources.", exception);
        }
    }

    public List<DeletedSourceInfo> findDeletedSources() {
        ensureDeletedSourcesTable();

        String sql = """
                SELECT
                    id,
                    old_source_id,
                    old_source_code,
                    fanpage_id,
                    page_code,
                    page_name,
                    source_name,
                    channel_name,
                    source_url,
                    source_type,
                    deleted_time
                FROM deleted_sources
                ORDER BY id DESC
                """;

        List<DeletedSourceInfo> deletedSources = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                DeletedSourceInfo item = new DeletedSourceInfo(
                        resultSet.getLong("id"),
                        readNullableLong(resultSet, "old_source_id"),
                        resultSet.getString("old_source_code"),
                        readNullableLong(resultSet, "fanpage_id"),
                        resultSet.getString("page_code"),
                        resultSet.getString("page_name"),
                        resultSet.getString("source_name"),
                        resultSet.getString("channel_name"),
                        resultSet.getString("source_url"),
                        resultSet.getString("source_type"),
                        resultSet.getString("deleted_time")
                );

                deletedSources.add(item);
            }

            return deletedSources;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách source đã xóa.", exception);
        }
    }

    public int deleteInactiveSourceToHistory(Long sourceId) {
        if (sourceId == null) {
            throw new RuntimeException("Source ID đang bị trống.");
        }

        ensureDeletedSourcesTable();

        String findSourceSql = """
            SELECT
                s.id,
                s.source_code,
                s.fanpage_id,
                s.source_name,
                s.channel_name,
                s.source_url,
                s.source_type,
                s.active,
                f.page_code,
                f.page_name
            FROM sources s
            LEFT JOIN fanpages f ON f.id = s.fanpage_id
            WHERE s.id = ?
            LIMIT 1
            """;

        String checkAlreadyDeletedSql = """
            SELECT COUNT(*) AS count_value
            FROM deleted_sources
            WHERE old_source_id = ?
            """;

        String insertDeletedSourceSql = """
            INSERT INTO deleted_sources (
                old_source_id,
                old_source_code,
                fanpage_id,
                page_code,
                page_name,
                source_name,
                channel_name,
                source_url,
                source_type,
                deleted_time
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
            """;

        String markSourceInactiveSql = """
            UPDATE sources
            SET active = 0
            WHERE id = ?
              AND active = 0
            """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try (
                    PreparedStatement findSourceStatement = connection.prepareStatement(findSourceSql);
                    PreparedStatement checkAlreadyDeletedStatement = connection.prepareStatement(checkAlreadyDeletedSql);
                    PreparedStatement insertDeletedSourceStatement = connection.prepareStatement(insertDeletedSourceSql);
                    PreparedStatement markSourceInactiveStatement = connection.prepareStatement(markSourceInactiveSql)
            ) {
                findSourceStatement.setLong(1, sourceId);

                try (ResultSet resultSet = findSourceStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new RuntimeException("Không tìm thấy source cần xóa.");
                    }

                    boolean active = resultSet.getInt("active") == 1;

                    if (active) {
                        throw new RuntimeException("Không được xóa source đang Active = Có. Hãy thay source mới trước, source cũ sẽ thành Active = Không rồi mới xóa được.");
                    }

                    checkAlreadyDeletedStatement.setLong(1, sourceId);

                    int alreadyDeletedCount = 0;

                    try (ResultSet checkResultSet = checkAlreadyDeletedStatement.executeQuery()) {
                        if (checkResultSet.next()) {
                            alreadyDeletedCount = checkResultSet.getInt("count_value");
                        }
                    }

                    if (alreadyDeletedCount <= 0) {
                        Long fanpageId = null;
                        long rawFanpageId = resultSet.getLong("fanpage_id");

                        if (!resultSet.wasNull()) {
                            fanpageId = rawFanpageId;
                        }

                        insertDeletedSourceStatement.setLong(1, resultSet.getLong("id"));
                        insertDeletedSourceStatement.setString(2, resultSet.getString("source_code"));
                        insertDeletedSourceStatement.setObject(3, fanpageId);
                        insertDeletedSourceStatement.setString(4, resultSet.getString("page_code"));
                        insertDeletedSourceStatement.setString(5, resultSet.getString("page_name"));
                        insertDeletedSourceStatement.setString(6, resultSet.getString("source_name"));
                        insertDeletedSourceStatement.setString(7, resultSet.getString("channel_name"));
                        insertDeletedSourceStatement.setString(8, resultSet.getString("source_url"));
                        insertDeletedSourceStatement.setString(9, resultSet.getString("source_type"));
                        insertDeletedSourceStatement.executeUpdate();
                    }
                }

                markSourceInactiveStatement.setLong(1, sourceId);
                int affectedCount = markSourceInactiveStatement.executeUpdate();

                connection.commit();

                return affectedCount <= 0 ? 1 : affectedCount;

            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }

        } catch (Exception exception) {
            throw new RuntimeException("Không thể xóa source.", exception);
        }
    }

    public Set<Path> findFanpageRelatedFolders(Long fanpageId) {
        if (fanpageId == null) {
            throw new RuntimeException("Fanpage ID đang bị trống.");
        }

        Set<Path> folders = new LinkedHashSet<>();

        collectFolderPathsIfColumnExists(
                folders,
                "video_batches",
                "raw_folder_path",
                "fanpage_id",
                fanpageId
        );

        collectFolderPathsIfColumnExists(
                folders,
                "video_batches",
                "edited_folder_path",
                "fanpage_id",
                fanpageId
        );

        return folders;
    }

    private void collectFolderPathsIfColumnExists(
            Set<Path> folders,
            String tableName,
            String folderColumnName,
            String fanpageColumnName,
            Long fanpageId
    ) {
        try (Connection connection = DatabaseManager.getConnection()) {
            if (!tableExists(connection, tableName)) {
                return;
            }

            if (!columnExists(connection, tableName, folderColumnName)) {
                return;
            }

            if (!columnExists(connection, tableName, fanpageColumnName)) {
                return;
            }

            String sql = "SELECT " + folderColumnName + " AS folder_path "
                    + "FROM " + tableName + " "
                    + "WHERE " + fanpageColumnName + " = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setLong(1, fanpageId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String folderPath = resultSet.getString("folder_path");

                        if (folderPath != null && !folderPath.isBlank()) {
                            folders.add(Path.of(folderPath));
                        }
                    }
                }
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách folder liên quan fanpage.", exception);
        }
    }

    public int hardDeleteFanpage(Long fanpageId) {
        if (fanpageId == null) {
            throw new RuntimeException("Fanpage ID đang bị trống.");
        }

        ensureDeletedSourcesTable();

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int totalDeletedRows = 0;

                totalDeletedRows += executeDeleteIfColumnsExist(
                        connection,
                        new String[][]{
                                {"upload_batch_items", "video_id"},
                                {"videos", "fanpage_id"}
                        },
                        """
                                DELETE FROM upload_batch_items
                                WHERE video_id IN (
                                    SELECT id
                                    FROM videos
                                    WHERE fanpage_id = ?
                                )
                                """,
                        fanpageId
                );

                totalDeletedRows += executeDeleteIfColumnsExist(
                        connection,
                        new String[][]{
                                {"upload_batch_items", "upload_batch_id"},
                                {"upload_batches", "fanpage_id"}
                        },
                        """
                                DELETE FROM upload_batch_items
                                WHERE upload_batch_id IN (
                                    SELECT id
                                    FROM upload_batches
                                    WHERE fanpage_id = ?
                                )
                                """,
                        fanpageId
                );

                totalDeletedRows += executeDeleteIfColumnsExist(
                        connection,
                        new String[][]{
                                {"upload_batches", "fanpage_id"}
                        },
                        """
                                DELETE FROM upload_batches
                                WHERE fanpage_id = ?
                                """,
                        fanpageId
                );

                totalDeletedRows += executeDeleteIfColumnsExist(
                        connection,
                        new String[][]{
                                {"video_files", "video_id"},
                                {"videos", "fanpage_id"}
                        },
                        """
                                DELETE FROM video_files
                                WHERE video_id IN (
                                    SELECT id
                                    FROM videos
                                    WHERE fanpage_id = ?
                                )
                                """,
                        fanpageId
                );

                totalDeletedRows += executeDeleteIfColumnsExist(
                        connection,
                        new String[][]{
                                {"videos", "fanpage_id"}
                        },
                        """
                                DELETE FROM videos
                                WHERE fanpage_id = ?
                                """,
                        fanpageId
                );

                totalDeletedRows += executeDeleteIfColumnsExist(
                        connection,
                        new String[][]{
                                {"video_batches", "fanpage_id"}
                        },
                        """
                                DELETE FROM video_batches
                                WHERE fanpage_id = ?
                                """,
                        fanpageId
                );

                totalDeletedRows += executeDeleteIfColumnsExist(
                        connection,
                        new String[][]{
                                {"download_batches", "fanpage_id"}
                        },
                        """
                                DELETE FROM download_batches
                                WHERE fanpage_id = ?
                                """,
                        fanpageId
                );

                totalDeletedRows += executeDeleteIfColumnsExist(
                        connection,
                        new String[][]{
                                {"deleted_sources", "fanpage_id"}
                        },
                        """
                                DELETE FROM deleted_sources
                                WHERE fanpage_id = ?
                                """,
                        fanpageId
                );

                totalDeletedRows += executeDeleteIfColumnsExist(
                        connection,
                        new String[][]{
                                {"sources", "fanpage_id"}
                        },
                        """
                                DELETE FROM sources
                                WHERE fanpage_id = ?
                                """,
                        fanpageId
                );

                totalDeletedRows += executeDeleteIfColumnsExist(
                        connection,
                        new String[][]{
                                {"fanpages", "id"}
                        },
                        """
                                DELETE FROM fanpages
                                WHERE id = ?
                                """,
                        fanpageId
                );

                connection.commit();

                return totalDeletedRows;

            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }

        } catch (Exception exception) {
            throw new RuntimeException("Không thể xóa toàn bộ fanpage.", exception);
        }
    }

    private int executeDeleteIfColumnsExist(
            Connection connection,
            String[][] tableColumnChecks,
            String sql,
            Long fanpageId
    ) throws SQLException {
        for (String[] check : tableColumnChecks) {
            String tableName = check[0];
            String columnName = check[1];

            if (!tableExists(connection, tableName)) {
                return 0;
            }

            if (!columnExists(connection, tableName, columnName)) {
                return 0;
            }
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, fanpageId);
            return preparedStatement.executeUpdate();
        }
    }

    private boolean tableExists(
            Connection connection,
            String tableName
    ) throws SQLException {
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

    private boolean columnExists(
            Connection connection,
            String tableName,
            String columnName
    ) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                String currentColumnName = resultSet.getString("name");

                if (columnName.equalsIgnoreCase(currentColumnName)) {
                    return true;
                }
            }

            return false;
        }
    }

    private Long readNullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);

        if (resultSet.wasNull()) {
            return null;
        }

        return value;
    }
}