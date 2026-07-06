package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemToolService {

    private static final String YT_DLP_RELEASE_API =
            "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest";

    private static final String YT_DLP_RELEASE_PAGE =
            "https://github.com/yt-dlp/yt-dlp/releases/latest";

    private static final String YT_DLP_EXE_DOWNLOAD =
            "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";

    private static final String FFMPEG_RELEASE_API =
            "https://api.github.com/repos/BtbN/FFmpeg-Builds/releases/latest";

    private static final String FFMPEG_RELEASE_PAGE =
            "https://github.com/BtbN/FFmpeg-Builds/releases/latest";

    private static final String FFMPEG_FALLBACK_DOWNLOAD =
            "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip";

    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String checkYtDlp() {
        StringBuilder builder = new StringBuilder();

        builder.append("KIỂM TRA YT-DLP").append(System.lineSeparator());
        builder.append("================").append(System.lineSeparator());

        Path ytDlpFile = AppPaths.ytDlpFile();

        builder.append("File local: ")
                .append(ytDlpFile.toAbsolutePath())
                .append(System.lineSeparator());

        if (!Files.exists(ytDlpFile)) {
            builder.append(System.lineSeparator());
            builder.append("Trạng thái local: CHƯA CÓ FILE yt-dlp.exe")
                    .append(System.lineSeparator());
            builder.append("Link tải yt-dlp.exe: ")
                    .append(YT_DLP_EXE_DOWNLOAD)
                    .append(System.lineSeparator());
            builder.append("Trang release: ")
                    .append(YT_DLP_RELEASE_PAGE)
                    .append(System.lineSeparator());
            return builder.toString();
        }

        if (!Files.isRegularFile(ytDlpFile)) {
            builder.append(System.lineSeparator());
            builder.append("Trạng thái local: Đường dẫn yt-dlp không phải file.")
                    .append(System.lineSeparator());
            return builder.toString();
        }

        CommandResult localVersionResult = runCommand(
                List.of(ytDlpFile.toAbsolutePath().toString(), "--version"),
                ytDlpFile.getParent(),
                20
        );

        String localVersion = localVersionResult.output().trim();

        builder.append("Version local: ")
                .append(emptyToDash(localVersion))
                .append(System.lineSeparator());

        builder.append("File sửa lần cuối: ")
                .append(formatLastModified(ytDlpFile))
                .append(System.lineSeparator());

        if (localVersionResult.exitCode() != 0) {
            builder.append(System.lineSeparator());
            builder.append("Cảnh báo: yt-dlp chạy không thành công.")
                    .append(System.lineSeparator());
            builder.append("Output:")
                    .append(System.lineSeparator())
                    .append(localVersionResult.output())
                    .append(System.lineSeparator());
        }

        builder.append(System.lineSeparator());
        builder.append("Đang kiểm tra bản mới online...")
                .append(System.lineSeparator());

        try {
            ReleaseInfo latestRelease = fetchGithubLatestRelease(
                    YT_DLP_RELEASE_API,
                    "yt-dlp.exe"
            );

            String remoteVersion = extractYtDlpVersion(latestRelease.tagName());
            if (remoteVersion.isBlank()) {
                remoteVersion = extractYtDlpVersion(latestRelease.name());
            }

            String downloadUrl = latestRelease.assetDownloadUrl();

            if (downloadUrl == null || downloadUrl.isBlank()) {
                downloadUrl = YT_DLP_EXE_DOWNLOAD;
            }

            builder.append("Version mới nhất online: ")
                    .append(emptyToDash(remoteVersion))
                    .append(System.lineSeparator());

            builder.append("Release: ")
                    .append(emptyToDash(latestRelease.name()))
                    .append(System.lineSeparator());

            builder.append("Ngày release: ")
                    .append(emptyToDash(latestRelease.publishedAt()))
                    .append(System.lineSeparator());

            int compareResult = compareVersion(localVersion, remoteVersion);

            builder.append(System.lineSeparator());

            if (compareResult < 0) {
                builder.append("KẾT LUẬN: CÓ BẢN YT-DLP MỚI HƠN.")
                        .append(System.lineSeparator());
                builder.append("M nên update yt-dlp.")
                        .append(System.lineSeparator());
                builder.append(System.lineSeparator());
                builder.append("Link tải trực tiếp yt-dlp.exe:")
                        .append(System.lineSeparator())
                        .append(downloadUrl)
                        .append(System.lineSeparator());
            } else if (compareResult == 0) {
                builder.append("KẾT LUẬN: yt-dlp đang là bản mới nhất.")
                        .append(System.lineSeparator());
            } else {
                builder.append("KẾT LUẬN: Version local đang cao hơn hoặc khác format version online.")
                        .append(System.lineSeparator());
            }

            builder.append(System.lineSeparator());
            builder.append("Trang release:")
                    .append(System.lineSeparator())
                    .append(YT_DLP_RELEASE_PAGE)
                    .append(System.lineSeparator());

        } catch (Exception exception) {
            builder.append(System.lineSeparator());
            builder.append("Không kiểm tra được bản mới online.")
                    .append(System.lineSeparator());
            builder.append("Lý do: ")
                    .append(exception.getMessage())
                    .append(System.lineSeparator());
            builder.append(System.lineSeparator());
            builder.append("M có thể tải thủ công tại:")
                    .append(System.lineSeparator())
                    .append(YT_DLP_EXE_DOWNLOAD)
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    public String updateYtDlp() {
        Path ytDlpFile = AppPaths.ytDlpFile();

        if (!Files.exists(ytDlpFile)) {
            return "Không tìm thấy yt-dlp.exe để update."
                    + System.lineSeparator()
                    + "File cần đặt tại: " + ytDlpFile.toAbsolutePath()
                    + System.lineSeparator()
                    + "Link tải trực tiếp:"
                    + System.lineSeparator()
                    + YT_DLP_EXE_DOWNLOAD;
        }

        CommandResult result = runCommand(
                List.of(ytDlpFile.toAbsolutePath().toString(), "-U"),
                ytDlpFile.getParent(),
                120
        );

        return "UPDATE YT-DLP"
                + System.lineSeparator()
                + "============="
                + System.lineSeparator()
                + "Exit code: " + result.exitCode()
                + System.lineSeparator()
                + System.lineSeparator()
                + result.output()
                + System.lineSeparator()
                + "Sau khi update, bấm lại Kiểm tra yt-dlp để xem version mới.";
    }

    public String checkFfmpeg() {
        StringBuilder builder = new StringBuilder();

        builder.append("KIỂM TRA FFMPEG").append(System.lineSeparator());
        builder.append("================").append(System.lineSeparator());

        Path ffmpegFile = AppPaths.ffmpegFile();

        builder.append("File local: ")
                .append(ffmpegFile.toAbsolutePath())
                .append(System.lineSeparator());

        if (!Files.exists(ffmpegFile)) {
            builder.append(System.lineSeparator());
            builder.append("Trạng thái local: CHƯA CÓ FILE ffmpeg.exe")
                    .append(System.lineSeparator());
            builder.append("Link tải FFmpeg Windows:")
                    .append(System.lineSeparator())
                    .append(FFMPEG_FALLBACK_DOWNLOAD)
                    .append(System.lineSeparator());
            builder.append("Trang release:")
                    .append(System.lineSeparator())
                    .append(FFMPEG_RELEASE_PAGE)
                    .append(System.lineSeparator());
            return builder.toString();
        }

        if (!Files.isRegularFile(ffmpegFile)) {
            builder.append(System.lineSeparator());
            builder.append("Trạng thái local: Đường dẫn FFmpeg không phải file.")
                    .append(System.lineSeparator());
            return builder.toString();
        }

        CommandResult localVersionResult = runCommand(
                List.of(ffmpegFile.toAbsolutePath().toString(), "-version"),
                ffmpegFile.getParent(),
                20
        );

        String firstLine = firstLine(localVersionResult.output());

        builder.append("Version local: ")
                .append(emptyToDash(firstLine))
                .append(System.lineSeparator());

        builder.append("File sửa lần cuối: ")
                .append(formatLastModified(ffmpegFile))
                .append(System.lineSeparator());

        if (localVersionResult.exitCode() != 0) {
            builder.append(System.lineSeparator());
            builder.append("Cảnh báo: FFmpeg chạy không thành công.")
                    .append(System.lineSeparator());
            builder.append("Output:")
                    .append(System.lineSeparator())
                    .append(localVersionResult.output())
                    .append(System.lineSeparator());
        }

        builder.append(System.lineSeparator());
        builder.append("Đang kiểm tra build mới online...")
                .append(System.lineSeparator());

        try {
            ReleaseInfo latestRelease = fetchGithubLatestRelease(
                    FFMPEG_RELEASE_API,
                    "ffmpeg-master-latest-win64-gpl.zip"
            );

            String downloadUrl = latestRelease.assetDownloadUrl();

            if (downloadUrl == null || downloadUrl.isBlank()) {
                downloadUrl = FFMPEG_FALLBACK_DOWNLOAD;
            }

            builder.append("Build mới nhất online: ")
                    .append(emptyToDash(latestRelease.name()))
                    .append(System.lineSeparator());

            builder.append("Tag: ")
                    .append(emptyToDash(latestRelease.tagName()))
                    .append(System.lineSeparator());

            builder.append("Ngày build online: ")
                    .append(emptyToDash(latestRelease.publishedAt()))
                    .append(System.lineSeparator());

            boolean onlineNewer = isRemotePublishedAfterLocalFile(
                    latestRelease.publishedAt(),
                    ffmpegFile
            );

            builder.append(System.lineSeparator());

            if (onlineNewer) {
                builder.append("KẾT LUẬN: CÓ BUILD FFMPEG MỚI HƠN FILE LOCAL.")
                        .append(System.lineSeparator());
                builder.append("M nên tải file zip mới, giải nén rồi thay ffmpeg.exe trong folder tools.")
                        .append(System.lineSeparator());
                builder.append(System.lineSeparator());
                builder.append("Link tải FFmpeg Windows zip:")
                        .append(System.lineSeparator())
                        .append(downloadUrl)
                        .append(System.lineSeparator());
            } else {
                builder.append("KẾT LUẬN: Chưa phát hiện build online mới hơn file local.")
                        .append(System.lineSeparator());
                builder.append("Nếu vẫn lỗi tải video, m vẫn có thể tải lại FFmpeg theo link bên dưới.")
                        .append(System.lineSeparator());
                builder.append(System.lineSeparator());
                builder.append("Link tải FFmpeg Windows zip:")
                        .append(System.lineSeparator())
                        .append(downloadUrl)
                        .append(System.lineSeparator());
            }

            builder.append(System.lineSeparator());
            builder.append("Trang release:")
                    .append(System.lineSeparator())
                    .append(FFMPEG_RELEASE_PAGE)
                    .append(System.lineSeparator());

        } catch (Exception exception) {
            builder.append(System.lineSeparator());
            builder.append("Không kiểm tra được build mới online.")
                    .append(System.lineSeparator());
            builder.append("Lý do: ")
                    .append(exception.getMessage())
                    .append(System.lineSeparator());
            builder.append(System.lineSeparator());
            builder.append("M có thể tải thủ công tại:")
                    .append(System.lineSeparator())
                    .append(FFMPEG_FALLBACK_DOWNLOAD)
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    public void openToolsFolder() {
        openFolder(AppPaths.TOOLS_DIR);
    }

    public void openFfmpegFolder() {
        openFolder(AppPaths.ffmpegFile().getParent());
    }

    public void openBackupDatabaseFolder() {
        openFolder(AppPaths.BACKUP_DATABASE_DIR);
    }

    private void openFolder(Path folder) {
        if (folder == null) {
            throw new RuntimeException("Đường dẫn folder đang bị trống.");
        }

        try {
            Files.createDirectories(folder);

            new ProcessBuilder(
                    "explorer.exe",
                    folder.toAbsolutePath().toString()
            ).start();

        } catch (IOException exception) {
            throw new RuntimeException(
                    "Không thể mở folder: " + folder.toAbsolutePath(),
                    exception
            );
        }
    }

    public String checkDatabase() {
        StringBuilder builder = new StringBuilder();

        Path databaseFile = AppPaths.databaseFile();

        builder.append("KIỂM TRA DATABASE").append(System.lineSeparator());
        builder.append("=================").append(System.lineSeparator());
        builder.append("File DB: ")
                .append(databaseFile.toAbsolutePath())
                .append(System.lineSeparator());

        if (!Files.exists(databaseFile)) {
            builder.append("Trạng thái: CHƯA CÓ FILE DATABASE.")
                    .append(System.lineSeparator());
            return builder.toString();
        }

        try {
            long size = Files.size(databaseFile);

            builder.append("Trạng thái: OK")
                    .append(System.lineSeparator());
            builder.append("Dung lượng: ")
                    .append(formatFileSize(size))
                    .append(System.lineSeparator());
            builder.append("Sửa lần cuối: ")
                    .append(formatLastModified(databaseFile))
                    .append(System.lineSeparator());

        } catch (IOException exception) {
            builder.append("Trạng thái: LỖI")
                    .append(System.lineSeparator());
            builder.append("Lý do: ")
                    .append(exception.getMessage())
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    public String checkAll() {
        return checkYtDlp()
                + System.lineSeparator()
                + System.lineSeparator()
                + "--------------------------------------------------"
                + System.lineSeparator()
                + System.lineSeparator()
                + checkFfmpeg()
                + System.lineSeparator()
                + System.lineSeparator()
                + "--------------------------------------------------"
                + System.lineSeparator()
                + System.lineSeparator()
                + checkDatabase();
    }

    private ReleaseInfo fetchGithubLatestRelease(
            String apiUrl,
            String preferredAssetName
    ) {
        String json = httpGet(apiUrl);

        String tagName = extractJsonString(json, "tag_name");
        String name = extractJsonString(json, "name");
        String htmlUrl = extractJsonString(json, "html_url");
        String publishedAt = extractJsonString(json, "published_at");

        String assetDownloadUrl = extractPreferredAssetUrl(json, preferredAssetName);

        return new ReleaseInfo(
                tagName,
                name,
                htmlUrl,
                publishedAt,
                assetDownloadUrl
        );
    }

    private String httpGet(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "auto-by-Hapro")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + response.statusCode());
            }

            return response.body();

        } catch (Exception exception) {
            throw new RuntimeException("Không gọi được API: " + url + " | " + exception.getMessage(), exception);
        }
    }

    private String extractPreferredAssetUrl(String json, String preferredAssetName) {
        List<String> urls = extractAllBrowserDownloadUrls(json);

        if (urls.isEmpty()) {
            return "";
        }

        String lowerPreferred = preferredAssetName.toLowerCase();

        for (String url : urls) {
            String lowerUrl = url.toLowerCase();

            if (lowerUrl.endsWith("/" + lowerPreferred)) {
                return url;
            }
        }

        for (String url : urls) {
            String lowerUrl = url.toLowerCase();

            if (lowerUrl.contains(lowerPreferred)) {
                return url;
            }
        }

        if (lowerPreferred.contains("ffmpeg")) {
            for (String url : urls) {
                String lowerUrl = url.toLowerCase();

                if (lowerUrl.contains("win64-gpl.zip")
                        && !lowerUrl.contains("shared")
                        && !lowerUrl.contains("lgpl")) {
                    return url;
                }
            }

            for (String url : urls) {
                String lowerUrl = url.toLowerCase();

                if (lowerUrl.contains("win64")
                        && lowerUrl.contains("gpl")
                        && lowerUrl.endsWith(".zip")) {
                    return url;
                }
            }
        }

        return urls.get(0);
    }

    private List<String> extractAllBrowserDownloadUrls(String json) {
        List<String> urls = new ArrayList<>();

        Pattern pattern = Pattern.compile(
                "\"browser_download_url\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\""
        );

        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            urls.add(unescapeJsonString(matcher.group(1)));
        }

        return urls;
    }

    private String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\""
        );

        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return unescapeJsonString(matcher.group(1));
        }

        return "";
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

    private String extractYtDlpVersion(String text) {
        if (text == null) {
            return "";
        }

        Pattern pattern = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2})");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return text.trim();
    }

    private int compareVersion(String localVersion, String remoteVersion) {
        List<Integer> localParts = parseVersionParts(localVersion);
        List<Integer> remoteParts = parseVersionParts(remoteVersion);

        if (localParts.isEmpty() || remoteParts.isEmpty()) {
            return 0;
        }

        int max = Math.max(localParts.size(), remoteParts.size());

        for (int index = 0; index < max; index++) {
            int local = index < localParts.size() ? localParts.get(index) : 0;
            int remote = index < remoteParts.size() ? remoteParts.get(index) : 0;

            if (local < remote) {
                return -1;
            }

            if (local > remote) {
                return 1;
            }
        }

        return 0;
    }

    private List<Integer> parseVersionParts(String version) {
        List<Integer> parts = new ArrayList<>();

        if (version == null || version.isBlank()) {
            return parts;
        }

        Matcher matcher = Pattern.compile("\\d+").matcher(version);

        while (matcher.find()) {
            try {
                parts.add(Integer.parseInt(matcher.group()));
            } catch (NumberFormatException ignored) {
                // Bỏ qua phần không parse được.
            }
        }

        return parts;
    }

    private boolean isRemotePublishedAfterLocalFile(String publishedAt, Path localFile) {
        if (publishedAt == null || publishedAt.isBlank()) {
            return false;
        }

        try {
            Instant remoteInstant = Instant.parse(publishedAt);
            Instant localInstant = Files.getLastModifiedTime(localFile).toInstant();

            return remoteInstant.isAfter(localInstant);

        } catch (Exception exception) {
            return false;
        }
    }

    private CommandResult runCommand(
            List<String> command,
            Path workingDirectory,
            int timeoutSeconds
    ) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            if (workingDirectory != null) {
                processBuilder.directory(workingDirectory.toFile());
            }

            Process process = processBuilder.start();

            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeoutSeconds * 1000L;

            while (process.isAlive()) {
                long runningMillis = System.currentTimeMillis() - startTime;

                if (runningMillis >= timeoutMillis) {
                    process.destroy();

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }

                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }

                    return new CommandResult(
                            -1,
                            "Lệnh chạy quá " + timeoutSeconds + " giây nên đã bị dừng."
                    );
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
                            "Lệnh bị ngắt khi đang chạy."
                    );
                }
            }

            String output = new String(process.getInputStream().readAllBytes());

            return new CommandResult(
                    process.exitValue(),
                    output
            );

        } catch (Exception exception) {
            return new CommandResult(
                    -1,
                    exception.getMessage()
            );
        }
    }

    private String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String[] lines = text.split("\\R");

        if (lines.length == 0) {
            return text.trim();
        }

        return lines[0].trim();
    }

    private String formatLastModified(Path file) {
        try {
            Instant instant = Files.getLastModifiedTime(file).toInstant();

            return LocalDateTime
                    .ofInstant(instant, ZoneId.systemDefault())
                    .format(DISPLAY_TIME_FORMAT);

        } catch (IOException exception) {
            return "";
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        double kb = bytes / 1024.0;

        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }

        double mb = kb / 1024.0;

        return String.format("%.2f MB", mb);
    }

    private String emptyToDash(String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }

        return text.trim();
    }

    private record ReleaseInfo(
            String tagName,
            String name,
            String htmlUrl,
            String publishedAt,
            String assetDownloadUrl
    ) {
    }

    private record CommandResult(
            int exitCode,
            String output
    ) {
    }
}