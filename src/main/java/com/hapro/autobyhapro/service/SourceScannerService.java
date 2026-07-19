package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.Source;
import com.hapro.autobyhapro.entity.SourceScanResult;
import com.hapro.autobyhapro.entity.VideoCandidate;
import com.hapro.autobyhapro.repository.VideoRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SourceScannerService {

    private static final int YT_DLP_TIMEOUT_SECONDS = 60;

    private final VideoRepository videoRepository = new VideoRepository();
    private final TikTokChannelResolverService tiktokChannelResolverService =
            new TikTokChannelResolverService();

    public SourceScanResult findNewVideos(Source source, int scanLimit) {
        return findNewVideos(
                source,
                1,
                Math.max(1, scanLimit)
        );
    }

    public SourceScanResult findNewVideos(
            Source source,
            int playlistStart,
            int playlistEnd
    ) {
        if (source == null) {
            throw new RuntimeException("Source đang bị trống.");
        }

        int safePlaylistStart = Math.max(1, playlistStart);
        int safePlaylistEnd = Math.max(safePlaylistStart, playlistEnd);
        int expectedScanCount = safePlaylistEnd - safePlaylistStart + 1;

        String sourceUrlForScan = tiktokChannelResolverService.resolveSourceUrlForUse(source);

        if (sourceUrlForScan == null || sourceUrlForScan.isBlank()) {
            sourceUrlForScan = source.getSourceUrl();
        }

        System.out.println(
                "Đang đọc video "
                        + safePlaylistStart
                        + " -> "
                        + safePlaylistEnd
                        + " từ source..."
        );
        System.out.println("URL dùng để quét: " + sourceUrlForScan);

        List<VideoCandidate> scannedVideos = listVideosByPlaylistRange(
                sourceUrlForScan,
                safePlaylistStart,
                safePlaylistEnd,
                YT_DLP_TIMEOUT_SECONDS
        );

        ScanFilterResult filterResult = filterNewVideos(
                source.getId(),
                scannedVideos,
                expectedScanCount
        );

        return new SourceScanResult(
                source,
                expectedScanCount,
                scannedVideos.size(),
                filterResult.skippedExistingCount(),
                filterResult.newVideos()
        );
    }

    private List<VideoCandidate> listVideosByPlaylistRange(
            String sourceUrl,
            int playlistStart,
            int playlistEnd,
            int timeoutSeconds
    ) {
        Path ytDlp = AppPaths.ytDlpFile();

        if (!Files.exists(ytDlp)) {
            throw new RuntimeException("Chưa có yt-dlp.exe tại: " + ytDlp.toAbsolutePath());
        }

        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new RuntimeException("Link source đang bị trống.");
        }

        List<String> command = new ArrayList<>();

        command.add(ytDlp.toAbsolutePath().toString());

        command.add("--encoding");
        command.add("utf-8");

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

        command.add("--playlist-start");
        command.add(String.valueOf(playlistStart));

        command.add("--playlist-end");
        command.add(String.valueOf(playlistEnd));

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

    private ScanFilterResult filterNewVideos(
            Long sourceId,
            List<VideoCandidate> scannedVideos,
            int maxNewVideoCount
    ) {
        Set<String> scannedIds = new LinkedHashSet<>();

        for (VideoCandidate video : scannedVideos) {
            scannedIds.add(video.getVideoId());
        }

        Set<String> existingIds = videoRepository.findExistingVideoIds(
                sourceId,
                scannedIds
        );

        List<VideoCandidate> newVideos = new ArrayList<>();
        int skippedExistingCount = 0;

        for (VideoCandidate video : scannedVideos) {
            if (existingIds.contains(video.getVideoId())) {
                skippedExistingCount++;
                continue;
            }

            if (newVideos.size() < maxNewVideoCount) {
                newVideos.add(video);
            }
        }

        return new ScanFilterResult(
                newVideos,
                skippedExistingCount
        );
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

            String lowerVideoId = videoId.toLowerCase();

            if (lowerVideoId.startsWith("warning:")
                    || lowerVideoId.startsWith("error:")
                    || lowerVideoId.startsWith("[")
                    || lowerVideoId.contains("unable to")) {
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

        result = Normalizer.normalize(result, Normalizer.Form.NFC);
        result = result.replace("�", "");
        result = result.replaceAll("\\s+", " ").trim();

        return result;
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

    private record ScanFilterResult(
            List<VideoCandidate> newVideos,
            int skippedExistingCount
    ) {
    }
}
