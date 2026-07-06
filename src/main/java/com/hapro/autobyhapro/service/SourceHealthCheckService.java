package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.SourceCheckResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceHealthCheckService {

    public List<SourceCheckResult> findActiveSourcesForCheck() {
        try (Connection connection = DatabaseManager.getConnection()) {
            if (!tableExists(connection, "sources")) {
                return List.of();
            }

            boolean fanpagesTableExists = tableExists(connection, "fanpages");
            boolean deletedSourcesTableExists = tableExists(connection, "deleted_sources");

            StringBuilder sqlBuilder = new StringBuilder();

            sqlBuilder.append("""
                    SELECT
                        s.id AS source_id,
                        s.source_code,
                        s.source_name,
                        s.source_type,
                        s.source_url,
                        s.channel_name,
                        s.active
                    """);

            if (fanpagesTableExists) {
                sqlBuilder.append("""
                        ,
                        f.page_code,
                        f.page_name
                        """);
            } else {
                sqlBuilder.append("""
                        ,
                        '' AS page_code,
                        '' AS page_name
                        """);
            }

            sqlBuilder.append("""
                    FROM sources s
                    """);

            if (fanpagesTableExists) {
                sqlBuilder.append("""
                        LEFT JOIN fanpages f ON f.id = s.fanpage_id
                        """);
            }

            sqlBuilder.append("""
                    WHERE s.active = 1
                    """);

            if (deletedSourcesTableExists) {
                sqlBuilder.append("""
                        AND NOT EXISTS (
                            SELECT 1
                            FROM deleted_sources ds
                            WHERE ds.old_source_id = s.id
                        )
                        """);
            }

            sqlBuilder.append("""
                    ORDER BY page_code ASC, s.source_code ASC
                    """);

            List<SourceCheckResult> sources = new ArrayList<>();

            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlBuilder.toString());
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    SourceCheckResult item = new SourceCheckResult(
                            resultSet.getLong("source_id"),
                            resultSet.getString("page_code"),
                            resultSet.getString("page_name"),
                            resultSet.getString("source_code"),
                            resultSet.getString("source_name"),
                            resultSet.getString("source_type"),
                            resultSet.getString("source_url"),
                            resultSet.getString("channel_name"),
                            resultSet.getInt("active") == 1,
                            "Chưa kiểm tra",
                            0,
                            0,
                            0,
                            ""
                    );

                    sources.add(item);
                }
            }

            return sources;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tải danh sách source active.", exception);
        }
    }

    public SourceCheckResult checkSource(SourceCheckResult source) {
        if (source == null) {
            throw new RuntimeException("Source đang bị trống.");
        }

        if (source.getSourceUrl() == null || source.getSourceUrl().isBlank()) {
            return source.withCheckResult(
                    "Lỗi",
                    0,
                    0,
                    0,
                    "Source chưa có link."
            );
        }

        Path ytDlpFile = AppPaths.ytDlpFile();

        if (!Files.exists(ytDlpFile)) {
            return source.withCheckResult(
                    "Lỗi",
                    0,
                    0,
                    0,
                    "Chưa có yt-dlp.exe tại:\n" + ytDlpFile.toAbsolutePath()
            );
        }

        Set<String> downloadedIds = findDownloadedVideoIds(source.getSourceId());

        List<String> command = List.of(
                ytDlpFile.toAbsolutePath().toString(),
                "--flat-playlist",
                "--dump-json",
                "--no-warnings",
                "--ignore-errors",
                source.getSourceUrl()
        );

        CommandResult commandResult = runCommand(
                command,
                ytDlpFile.getParent(),
                300
        );

        Set<String> scannedVideoIds = extractVideoIdsFromYtDlpOutput(commandResult.output());

        int totalFound = scannedVideoIds.size();
        int alreadyDownloaded = 0;

        for (String videoId : scannedVideoIds) {
            if (downloadedIds.contains(videoId)) {
                alreadyDownloaded++;
            }
        }

        int notYetDownloaded = totalFound - alreadyDownloaded;

        String status;
        String message;

        if (commandResult.timeout()) {
            status = "Quá lâu";
            message = "Lệnh kiểm tra chạy quá 300 giây nên đã bị dừng.\n"
                    + "Source này có thể quá nhiều video hoặc mạng đang chậm.\n\n"
                    + shortOutput(commandResult.output());
        } else if (totalFound > 0 && commandResult.exitCode() == 0) {
            status = "OK";
            message = "Source kiểm tra OK.\n"
                    + "Tổng video quét được: " + totalFound + "\n"
                    + "Đã có trong DB: " + alreadyDownloaded + "\n"
                    + "Chưa tải: " + notYetDownloaded;
        } else if (totalFound > 0) {
            status = "Cảnh báo";
            message = "Có quét được video nhưng yt-dlp trả về exit code: "
                    + commandResult.exitCode()
                    + "\n\n"
                    + "Tổng video quét được: " + totalFound + "\n"
                    + "Đã có trong DB: " + alreadyDownloaded + "\n"
                    + "Chưa tải: " + notYetDownloaded + "\n\n"
                    + shortOutput(commandResult.output());
        } else {
            status = "Lỗi";
            message = "Không quét được video nào từ source này.\n"
                    + "Exit code: " + commandResult.exitCode()
                    + "\n\n"
                    + shortOutput(commandResult.output());
        }

        return source.withCheckResult(
                status,
                totalFound,
                alreadyDownloaded,
                notYetDownloaded,
                message
        );
    }

    private Set<String> findDownloadedVideoIds(Long sourceId) {
        Set<String> ids = new LinkedHashSet<>();

        if (sourceId == null) {
            return ids;
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            if (!tableExists(connection, "videos")) {
                return ids;
            }

            if (!columnExists(connection, "videos", "source_id")) {
                return ids;
            }

            String videoIdColumn;

            if (columnExists(connection, "videos", "platform_video_id")) {
                videoIdColumn = "platform_video_id";
            } else if (columnExists(connection, "videos", "video_id")) {
                videoIdColumn = "video_id";
            } else {
                return ids;
            }

            String sql = "SELECT " + videoIdColumn + " AS video_id FROM videos WHERE source_id = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setLong(1, sourceId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String videoId = resultSet.getString("video_id");

                        if (videoId != null && !videoId.isBlank()) {
                            ids.add(videoId.trim());
                        }
                    }
                }
            }

            return ids;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể đọc video ID đã tải của source.", exception);
        }
    }

    private Set<String> extractVideoIdsFromYtDlpOutput(String output) {
        Set<String> ids = new LinkedHashSet<>();

        if (output == null || output.isBlank()) {
            return ids;
        }

        Pattern pattern = Pattern.compile(
                "\"id\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\""
        );

        Matcher matcher = pattern.matcher(output);

        while (matcher.find()) {
            String id = unescapeJsonString(matcher.group(1));

            if (id != null && !id.isBlank()) {
                ids.add(id.trim());
            }
        }

        return ids;
    }

    private CommandResult runCommand(
            List<String> command,
            Path workingDirectory,
            int timeoutSeconds
    ) {
        StringBuilder outputBuilder = new StringBuilder();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            if (workingDirectory != null) {
                processBuilder.directory(workingDirectory.toFile());
            }

            Process process = processBuilder.start();

            Thread outputReaderThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
                )) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append(System.lineSeparator());
                    }

                } catch (IOException exception) {
                    outputBuilder.append("Không đọc được output: ")
                            .append(exception.getMessage())
                            .append(System.lineSeparator());
                }
            });

            outputReaderThread.setDaemon(true);
            outputReaderThread.start();

            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeoutSeconds * 1000L;
            boolean timeout = false;

            while (process.isAlive()) {
                long runningMillis = System.currentTimeMillis() - startTime;

                if (runningMillis >= timeoutMillis) {
                    timeout = true;
                    process.destroy();

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }

                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }

                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();

                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }

                    return new CommandResult(
                            -1,
                            outputBuilder.toString() + "\nLệnh bị ngắt khi đang chạy.",
                            false
                    );
                }
            }

            try {
                outputReaderThread.join(1000);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }

            int exitCode = timeout ? -1 : process.exitValue();

            return new CommandResult(
                    exitCode,
                    outputBuilder.toString(),
                    timeout
            );

        } catch (Exception exception) {
            return new CommandResult(
                    -1,
                    outputBuilder + "\n" + exception.getMessage(),
                    false
            );
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
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

    private String shortOutput(String output) {
        if (output == null || output.isBlank()) {
            return "Không có output.";
        }

        String cleanOutput = output.trim();

        if (cleanOutput.length() <= 2000) {
            return cleanOutput;
        }

        return cleanOutput.substring(0, 2000)
                + "\n\n... Output quá dài nên đã rút gọn.";
    }

    private String unescapeJsonString(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private record CommandResult(
            int exitCode,
            String output,
            boolean timeout
    ) {
    }
}