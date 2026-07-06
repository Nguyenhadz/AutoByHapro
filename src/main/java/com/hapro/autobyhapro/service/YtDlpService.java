package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.VideoCandidate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class YtDlpService {

    private static final int DEFAULT_TIMEOUT_SECONDS = 90;

    public List<VideoCandidate> listSampleVideos(String sourceUrl, int limit) {
        return listVideos(sourceUrl, limit, 60);
    }

    public List<VideoCandidate> listVideos(String sourceUrl, int scanLimit) {
        return listVideos(sourceUrl, scanLimit, DEFAULT_TIMEOUT_SECONDS);
    }

    public List<VideoCandidate> listVideos(String sourceUrl, int scanLimit, int timeoutSeconds) {
        Path ytDlp = AppPaths.ytDlpFile();

        if (!Files.exists(ytDlp)) {
            throw new RuntimeException("Chưa có yt-dlp.exe tại: " + ytDlp.toAbsolutePath());
        }

        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new RuntimeException("Link source đang bị trống.");
        }

        if (scanLimit <= 0) {
            scanLimit = 10;
        }

        if (timeoutSeconds <= 0) {
            timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        }

        List<String> command = new ArrayList<>();

        command.add(ytDlp.toAbsolutePath().toString());

        // Ép yt-dlp xuất UTF-8 để giữ tiếng Việt có dấu.
        command.add("--encoding");
        command.add("utf-8");

        // Ưu tiên metadata tiếng Việt, tránh YouTube tự trả title tiếng Anh nếu có bản dịch.
        command.add("--extractor-args");
        command.add("youtube:lang=vi");

        command.add("--no-warnings");
        command.add("--ignore-errors");
        command.add("--flat-playlist");

        addYoutubeSupportOptions(command, sourceUrl);

        command.add("--socket-timeout");
        command.add("15");

        command.add("--retries");
        command.add("1");

        command.add("--playlist-end");
        command.add(String.valueOf(scanLimit));

        command.add("--print");
        command.add("%(id)s\t%(title)s\t%(webpage_url)s");

        command.add(sourceUrl);

        CommandResult result = runCommand(command, timeoutSeconds);

        if (!result.success()) {
            throw new RuntimeException("""
                    yt-dlp đọc source bị lỗi hoặc chạy quá lâu.

                    Exit code: %d

                    Kết quả:
                    %s
                    """.formatted(result.exitCode(), result.output()));
        }

        return parseVideoCandidates(result.output());
    }

    private void addYoutubeSupportOptions(List<String> command, String url) {
        if (!isYoutubeUrl(url)) {
            return;
        }

        addYoutubeCookiesIfExists(command);

        command.add("--remote-components");
        command.add("ejs:github");

        command.add("--sleep-requests");
        command.add("1");

        command.add("--sleep-interval");
        command.add("2");

        command.add("--max-sleep-interval");
        command.add("5");
    }

    private void addYoutubeCookiesIfExists(List<String> command) {
        Path cookiesFile = AppPaths.YOUTUBE_COOKIES_FILE;

        if (!Files.exists(cookiesFile)) {
            return;
        }

        command.add("--cookies");
        command.add(cookiesFile.toAbsolutePath().toString());
    }

    private boolean isYoutubeUrl(String url) {
        if (url == null) {
            return false;
        }

        String lowerUrl = url.toLowerCase();

        return lowerUrl.contains("youtube.com")
                || lowerUrl.contains("youtu.be");
    }

    public boolean isRealTikTokVideoUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        Path ytDlp = AppPaths.ytDlpFile();

        if (!Files.exists(ytDlp)) {
            throw new RuntimeException("Chưa có yt-dlp.exe tại: " + ytDlp.toAbsolutePath());
        }

        List<String> command = new ArrayList<>();

        command.add(ytDlp.toAbsolutePath().toString());

        command.add("--encoding");
        command.add("utf-8");

        command.add("--no-warnings");
        command.add("--skip-download");

        command.add("--socket-timeout");
        command.add("20");

        command.add("--retries");
        command.add("1");

        command.add("-F");
        command.add(url);

        CommandResult result = runCommand(command, 90);

        if (!result.success()) {
            return false;
        }

        return hasRealVideoStream(result.output());
    }

    public String classifyTikTokContentUrl(String url) {
        if (url == null || url.isBlank()) {
            return "UNKNOWN";
        }

        Path ytDlp = AppPaths.ytDlpFile();

        if (!Files.exists(ytDlp)) {
            throw new RuntimeException("Chưa có yt-dlp.exe tại: " + ytDlp.toAbsolutePath());
        }

        List<String> command = new ArrayList<>();

        command.add(ytDlp.toAbsolutePath().toString());

        command.add("--encoding");
        command.add("utf-8");

        command.add("--no-warnings");
        command.add("--skip-download");

        command.add("--socket-timeout");
        command.add("20");

        command.add("--retries");
        command.add("1");

        command.add("-F");
        command.add(url);

        CommandResult result = runCommand(command, 90);

        if (!result.success()) {
            return "ERROR";
        }

        return detectTikTokContentTypeFromFormatOutput(result.output());
    }

    private String detectTikTokContentTypeFromFormatOutput(String output) {
        if (output == null || output.isBlank()) {
            return "UNKNOWN";
        }

        if (hasRealVideoStream(output)) {
            return "VIDEO";
        }

        String lowerOutput = output.toLowerCase();

        if (lowerOutput.contains("audio only")) {
            return "AUDIO_ONLY";
        }

        if (lowerOutput.contains("image")
                || lowerOutput.contains("images")
                || lowerOutput.contains("storyboard")
                || lowerOutput.contains("mhtml")) {
            return "PHOTO";
        }

        return "UNKNOWN";
    }

    private boolean hasRealVideoStream(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }

        String[] lines = output.split("\\R");

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String lowerLine = line.toLowerCase();

            if (!lowerLine.contains("mp4")) {
                continue;
            }

            if (lowerLine.contains("audio only")
                    || lowerLine.contains("storyboard")
                    || lowerLine.contains("image")
                    || lowerLine.contains("images")
                    || lowerLine.contains("mhtml")) {
                continue;
            }

            if (line.matches("(?i).*\\b\\d{2,5}x\\d{2,5}\\b.*")) {
                return true;
            }

            if (lowerLine.contains("h264")
                    || lowerLine.contains("avc1")
                    || lowerLine.contains("h265")
                    || lowerLine.contains("hevc")) {
                return true;
            }
        }

        return false;
    }

    private List<VideoCandidate> parseVideoCandidates(String output) {
        List<VideoCandidate> videos = new ArrayList<>();

        if (output == null || output.isBlank()) {
            return videos;
        }

        String[] lines = output.split("\\R");

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String[] parts = line.split("\\t", 3);

            String videoId = parts.length > 0 ? parts[0].trim() : "";
            String title = parts.length > 1 ? normalizeTitle(parts[1]) : "";
            String url = parts.length > 2 ? parts[2].trim() : "";

            if (videoId.isBlank()) {
                continue;
            }

            videos.add(new VideoCandidate(videoId, title, url));
        }

        return videos;
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        String result = title.trim();

        // Giữ Unicode tiếng Việt ở dạng chuẩn.
        result = Normalizer.normalize(result, Normalizer.Form.NFC);

        // Xóa ký tự lỗi nếu có, không xóa dấu tiếng Việt.
        result = result.replace("�", "");

        // Gom khoảng trắng.
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    private void addBundledDenoToPath(ProcessBuilder processBuilder) {
        Path denoFile = AppPaths.denoFile();

        if (!Files.exists(denoFile)) {
            return;
        }

        Path denoFolder = denoFile.getParent();

        String currentPath = processBuilder.environment().getOrDefault("PATH", "");

        processBuilder.environment().put(
                "PATH",
                denoFolder.toAbsolutePath() + ";" + currentPath
        );
    }

    private CommandResult runCommand(List<String> command, int timeoutSeconds) {
        Process process = null;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            processBuilder.directory(AppPaths.rootDir().toFile());
            addBundledDenoToPath(processBuilder);

            // Ép Python/yt-dlp dùng UTF-8 khi chạy từ Java trên Windows.
            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            processBuilder.environment().put("PYTHONUTF8", "1");
            processBuilder.environment().put("LANG", "vi_VN.UTF-8");
            processBuilder.environment().put("LC_ALL", "vi_VN.UTF-8");

            process = processBuilder.start();

            StringBuilder outputBuilder = new StringBuilder();

            Process finalProcess = process;

            Thread outputReaderThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(finalProcess.getInputStream(), StandardCharsets.UTF_8)
                )) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append(System.lineSeparator());
                    }
                } catch (Exception exception) {
                    outputBuilder.append("Không đọc được output: ")
                            .append(exception.getMessage())
                            .append(System.lineSeparator());
                }
            });

            outputReaderThread.start();

            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeoutSeconds * 1000L;

            while (process.isAlive()) {
                long runningTime = System.currentTimeMillis() - startTime;

                if (runningTime > timeoutMillis) {
                    process.destroyForcibly();

                    return new CommandResult(
                            false,
                            -1,
                            "Lệnh yt-dlp chạy quá " + timeoutSeconds + " giây nên tool đã tự dừng.\n"
                                    + "Source có thể quá chậm, link chưa đúng, mạng yếu hoặc nền tảng đang chặn."
                    );
                }

                Thread.sleep(300);
            }

            int exitCode = process.exitValue();

            outputReaderThread.join(3000);

            return new CommandResult(
                    exitCode == 0,
                    exitCode,
                    outputBuilder.toString()
            );

        } catch (Exception exception) {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }

            return new CommandResult(
                    false,
                    -1,
                    exception.getMessage()
            );
        }
    }

    private record CommandResult(
            boolean success,
            int exitCode,
            String output
    ) {
    }
}