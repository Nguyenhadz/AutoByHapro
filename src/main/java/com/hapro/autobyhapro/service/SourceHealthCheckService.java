package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.SourceCheckResult;

import java.io.BufferedReader;
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

public class SourceHealthCheckService {

    private static final int NOT_YET_DOWNLOADED_LIMIT = 500;
    private static final int CHECK_TIMEOUT_SECONDS = 300;

    private final TikTokChannelResolverService tiktokChannelResolverService = new TikTokChannelResolverService();

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
                        , f.page_code,
                          f.page_name
                        """);
            } else {
                sqlBuilder.append("""
                        , '' AS page_code,
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
            return source.withCheckResult("Lỗi", 0, 0, 0, "Source chưa có link.");
        }

        Path ytDlpFile = AppPaths.ytDlpFile();

        if (!Files.exists(ytDlpFile)) {
            return source.withCheckResult("Lỗi", 0, 0, 0, "Chưa có yt-dlp.exe tại:\n" + ytDlpFile.toAbsolutePath());
        }

        String sourceUrlForCheck = tiktokChannelResolverService.resolveSourceUrlForUse(
                source.getSourceId(),
                source.getSourceType(),
                source.getSourceUrl()
        );

        if (sourceUrlForCheck == null || sourceUrlForCheck.isBlank()) {
            sourceUrlForCheck = source.getSourceUrl();
        }

        Set<String> downloadedIds = findDownloadedVideoIds(source.getSourceId());

        List<String> command = new ArrayList<>();
        command.add(ytDlpFile.toAbsolutePath().toString());
        command.add("--flat-playlist");
        command.add("--print");
        command.add("%(id)s");
        command.add("--no-warnings");
        command.add("--ignore-errors");
        command.add(sourceUrlForCheck);

        SourceScanResult scanResult = runScanCommand(command, ytDlpFile.getParent(), CHECK_TIMEOUT_SECONDS, downloadedIds);

        int totalFound = scanResult.totalFoundCount();
        int alreadyDownloaded = scanResult.alreadyDownloadedCount();
        int notYetDownloaded = scanResult.notYetDownloadedCount();

        String messagePrefix = "";

        if (!sourceUrlForCheck.equals(source.getSourceUrl())) {
            messagePrefix = "Đang dùng TikTok channel_id đã resolve:\n" + sourceUrlForCheck + "\n\n";
        }

        if (scanResult.limitReached()) {
            return source.withCheckResult(
                    "OK",
                    totalFound,
                    alreadyDownloaded,
                    NOT_YET_DOWNLOADED_LIMIT,
                    messagePrefix
                            + "Source kiểm tra OK.\n"
                            + "Tool đã dừng quét sớm vì đã tìm được từ "
                            + NOT_YET_DOWNLOADED_LIMIT
                            + " video chưa tải trở lên.\n"
                            + "Không cần quét hết kênh để tránh chạy quá lâu.\n\n"
                            + "Tổng video đã quét: "
                            + totalFound
                            + "\nĐã có trong DB: "
                            + alreadyDownloaded
                            + "\nChưa tải: từ "
                            + NOT_YET_DOWNLOADED_LIMIT
                            + " video trở lên."
            );
        }

        if (scanResult.timeout()) {
            return source.withCheckResult(
                    "Quá lâu",
                    totalFound,
                    alreadyDownloaded,
                    notYetDownloaded,
                    messagePrefix
                            + "Lệnh kiểm tra chạy quá "
                            + CHECK_TIMEOUT_SECONDS
                            + " giây nên đã bị dừng.\n"
                            + "Kết quả bên dưới chỉ là phần đã quét được trước khi dừng.\n\n"
                            + "Tổng video đã quét: "
                            + totalFound
                            + "\nĐã có trong DB: "
                            + alreadyDownloaded
                            + "\nChưa tải: "
                            + notYetDownloaded
                            + "\n\n"
                            + shortOutput(scanResult.output())
            );
        }

        if (totalFound > 0 && scanResult.exitCode() == 0) {
            return source.withCheckResult(
                    "OK",
                    totalFound,
                    alreadyDownloaded,
                    notYetDownloaded,
                    messagePrefix
                            + "Source kiểm tra OK.\n"
                            + "Tổng video quét được: "
                            + totalFound
                            + "\nĐã có trong DB: "
                            + alreadyDownloaded
                            + "\nChưa tải: "
                            + notYetDownloaded
            );
        }

        if (totalFound > 0) {
            return source.withCheckResult(
                    "Cảnh báo",
                    totalFound,
                    alreadyDownloaded,
                    notYetDownloaded,
                    messagePrefix
                            + "Có quét được video nhưng yt-dlp trả về exit code: "
                            + scanResult.exitCode()
                            + "\n\nTổng video quét được: "
                            + totalFound
                            + "\nĐã có trong DB: "
                            + alreadyDownloaded
                            + "\nChưa tải: "
                            + notYetDownloaded
                            + "\n\n"
                            + shortOutput(scanResult.output())
            );
        }

        return source.withCheckResult(
                "Lỗi",
                totalFound,
                alreadyDownloaded,
                notYetDownloaded,
                messagePrefix
                        + "Không quét được video nào từ source này.\n"
                        + "Exit code: "
                        + scanResult.exitCode()
                        + "\n\n"
                        + shortOutput(scanResult.output())
        );
    }

    private Set<String> findDownloadedVideoIds(Long sourceId) {
        Set<String> ids = new LinkedHashSet<>();

        if (sourceId == null) {
            return ids;
        }

        try (Connection connection = DatabaseManager.getConnection()) {
            if (!tableExists(connection, "videos") || !columnExists(connection, "videos", "source_id")) {
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

    private SourceScanResult runScanCommand(List<String> command, Path workingDirectory, int timeoutSeconds, Set<String> downloadedIds) {
        Set<String> scannedIds = new LinkedHashSet<>();
        StringBuilder outputBuilder = new StringBuilder();

        Process process = null;
        int alreadyDownloadedCount = 0;
        int notYetDownloadedCount = 0;
        boolean limitReached = false;
        boolean timeout = false;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            if (workingDirectory != null) {
                processBuilder.directory(workingDirectory.toFile());
            }

            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            processBuilder.environment().put("PYTHONUTF8", "1");

            process = processBuilder.start();

            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeoutSeconds * 1000L;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                while (true) {
                    boolean readSomething = false;

                    while (reader.ready()) {
                        String line = reader.readLine();

                        if (line == null) {
                            break;
                        }

                        readSomething = true;
                        appendLimitedOutput(outputBuilder, line);

                        String videoId = extractVideoIdFromPrintedLine(line);

                        if (videoId == null || videoId.isBlank()) {
                            continue;
                        }

                        if (!scannedIds.add(videoId)) {
                            continue;
                        }

                        if (downloadedIds != null && downloadedIds.contains(videoId)) {
                            alreadyDownloadedCount++;
                        } else {
                            notYetDownloadedCount++;
                        }

                        if (notYetDownloadedCount >= NOT_YET_DOWNLOADED_LIMIT) {
                            limitReached = true;
                            destroyProcess(process);
                            break;
                        }
                    }

                    if (limitReached) {
                        break;
                    }

                    if (!process.isAlive()) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            appendLimitedOutput(outputBuilder, line);

                            String videoId = extractVideoIdFromPrintedLine(line);

                            if (videoId == null || videoId.isBlank()) {
                                continue;
                            }

                            if (!scannedIds.add(videoId)) {
                                continue;
                            }

                            if (downloadedIds != null && downloadedIds.contains(videoId)) {
                                alreadyDownloadedCount++;
                            } else {
                                notYetDownloadedCount++;
                            }

                            if (notYetDownloadedCount >= NOT_YET_DOWNLOADED_LIMIT) {
                                limitReached = true;
                                break;
                            }
                        }

                        break;
                    }

                    long runningMillis = System.currentTimeMillis() - startTime;

                    if (runningMillis >= timeoutMillis) {
                        timeout = true;
                        destroyProcess(process);
                        break;
                    }

                    if (!readSomething) {
                        Thread.sleep(100);
                    }
                }
            }

            int exitCode;

            if (timeout || limitReached) {
                exitCode = -1;
            } else {
                exitCode = process.exitValue();
            }

            return new SourceScanResult(exitCode, outputBuilder.toString(), timeout, limitReached, scannedIds.size(), alreadyDownloadedCount, notYetDownloadedCount);

        } catch (Exception exception) {
            if (process != null && process.isAlive()) {
                destroyProcess(process);
            }

            return new SourceScanResult(-1, outputBuilder + "\n" + exception.getMessage(), false, limitReached, scannedIds.size(), alreadyDownloadedCount, notYetDownloadedCount);
        }
    }

    private String extractVideoIdFromPrintedLine(String line) {
        if (line == null) {
            return "";
        }

        String cleanLine = line.trim();

        if (cleanLine.isBlank()) {
            return "";
        }

        String lowerLine = cleanLine.toLowerCase();

        if (lowerLine.startsWith("warning:")
                || lowerLine.startsWith("error:")
                || lowerLine.startsWith("[")
                || lowerLine.contains("unable to")
                || lowerLine.contains("http error")) {
            return "";
        }

        if (!cleanLine.matches("[A-Za-z0-9_-]{4,80}")) {
            return "";
        }

        return cleanLine;
    }

    private void appendLimitedOutput(StringBuilder outputBuilder, String line) {
        if (line == null) {
            return;
        }

        if (outputBuilder.length() >= 4000) {
            return;
        }

        outputBuilder.append(line).append(System.lineSeparator());
    }

    private void destroyProcess(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }

        process.destroy();

        try {
            Thread.sleep(300);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }

        if (process.isAlive()) {
            process.destroyForcibly();
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

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
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

        return cleanOutput.substring(0, 2000) + "\n\n...\nOutput quá dài nên đã rút gọn.";
    }

    private record SourceScanResult(int exitCode, String output, boolean timeout, boolean limitReached, int totalFoundCount, int alreadyDownloadedCount, int notYetDownloadedCount) {
    }
}
