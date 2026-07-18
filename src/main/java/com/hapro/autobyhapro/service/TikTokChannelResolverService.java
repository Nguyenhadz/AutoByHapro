package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.Source;
import com.hapro.autobyhapro.entity.VideoCandidate;
import com.hapro.autobyhapro.repository.SourceRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TikTokChannelResolverService {

    private static final int SAMPLE_VIDEO_COUNT = 5;
    private static final int RESOLVE_TIMEOUT_SECONDS = 90;

    private final YtDlpService ytDlpService = new YtDlpService();
    private final SourceRepository sourceRepository = new SourceRepository();

    public String resolveSourceUrlForUse(Source source) {
        if (source == null) {
            return "";
        }

        return resolveSourceUrlForUse(
                source.getId(),
                source.getSourceType(),
                source.getSourceUrl(),
                source.getResolvedSourceUrl()
        );
    }

    public String resolveSourceUrlForUse(Long sourceId, String sourceType, String sourceUrl) {
        String resolvedSourceUrl = "";

        if (sourceId != null) {
            resolvedSourceUrl = sourceRepository.findResolvedSourceUrlById(sourceId);
        }

        return resolveSourceUrlForUse(sourceId, sourceType, sourceUrl, resolvedSourceUrl);
    }

    public String resolveSourceUrlForUse(
            Long sourceId,
            String sourceType,
            String sourceUrl,
            String currentResolvedSourceUrl
    ) {
        if (!isTikTokSource(sourceType, sourceUrl)) {
            return safeTrim(sourceUrl);
        }

        String cleanSourceUrl = safeTrim(sourceUrl);

        if (cleanSourceUrl.isBlank()) {
            return "";
        }

        if (isResolvedTikTokUserUrl(cleanSourceUrl)) {
            String channelId = cleanSourceUrl.substring("tiktokuser:".length()).trim();

            if (isValidTikTokChannelId(channelId) && sourceId != null) {
                sourceRepository.updateTikTokResolvedInfo(
                        sourceId,
                        channelId,
                        cleanSourceUrl,
                        "Source đã dùng sẵn dạng tiktokuser:channel_id."
                );
            }

            return cleanSourceUrl;
        }

        if (currentResolvedSourceUrl != null
                && !currentResolvedSourceUrl.isBlank()
                && isResolvedTikTokUserUrl(currentResolvedSourceUrl)) {
            return currentResolvedSourceUrl.trim();
        }

        try {
            TikTokResolveResult result = resolveFromUsernameSource(cleanSourceUrl);

            if (sourceId != null) {
                sourceRepository.updateTikTokResolvedInfo(
                        sourceId,
                        result.channelId(),
                        result.resolvedSourceUrl(),
                        "Đã tự lấy TikTok channel_id từ video mẫu: " + result.sampleVideoUrl()
                );
            }

            return result.resolvedSourceUrl();

        } catch (Exception exception) {
            if (sourceId != null) {
                sourceRepository.updateTikTokResolveError(sourceId, exception.getMessage());
            }

            return cleanSourceUrl;
        }
    }

    public TikTokResolveResult resolveFromUsernameSource(String sourceUrl) {
        String cleanSourceUrl = safeTrim(sourceUrl);

        if (cleanSourceUrl.isBlank()) {
            throw new RuntimeException("Link TikTok source đang bị trống.");
        }

        if (isResolvedTikTokUserUrl(cleanSourceUrl)) {
            String channelId = cleanSourceUrl.substring("tiktokuser:".length()).trim();

            if (!isValidTikTokChannelId(channelId)) {
                throw new RuntimeException("TikTok channel_id trong source không hợp lệ.");
            }

            return new TikTokResolveResult(channelId, cleanSourceUrl, "");
        }

        List<VideoCandidate> sampleVideos = ytDlpService.listVideos(
                cleanSourceUrl,
                SAMPLE_VIDEO_COUNT,
                RESOLVE_TIMEOUT_SECONDS
        );

        if (sampleVideos == null || sampleVideos.isEmpty()) {
            throw new RuntimeException("Không lấy được video mẫu từ TikTok source, nên chưa thể lấy channel_id.");
        }

        String lastError = "";

        for (VideoCandidate sampleVideo : sampleVideos) {
            String sampleVideoUrl = buildSampleVideoUrl(cleanSourceUrl, sampleVideo);

            if (sampleVideoUrl.isBlank()) {
                continue;
            }

            try {
                String channelId = readTikTokChannelIdFromVideoUrl(sampleVideoUrl);

                if (!isValidTikTokChannelId(channelId)) {
                    continue;
                }

                return new TikTokResolveResult(
                        channelId,
                        "tiktokuser:" + channelId,
                        sampleVideoUrl
                );

            } catch (Exception exception) {
                lastError = exception.getMessage();
            }
        }

        throw new RuntimeException(
                "Đã tìm được video mẫu nhưng không lấy được TikTok channel_id."
                        + (lastError == null || lastError.isBlank() ? "" : "\n" + lastError)
        );
    }

    private String readTikTokChannelIdFromVideoUrl(String videoUrl) {
        Path ytDlp = AppPaths.ytDlpFile();

        if (!Files.exists(ytDlp)) {
            throw new RuntimeException("Chưa có yt-dlp.exe tại: " + ytDlp.toAbsolutePath());
        }

        List<String> command = new ArrayList<>();
        command.add(ytDlp.toAbsolutePath().toString());
        command.add("--encoding");
        command.add("utf-8");
        command.add("--no-warnings");
        command.add("--no-playlist");
        command.add("--skip-download");
        command.add("--socket-timeout");
        command.add("20");
        command.add("--retries");
        command.add("1");
        command.add("--print");
        command.add("%(channel_id)s");
        command.add(videoUrl);

        CommandResult result = runCommand(command, RESOLVE_TIMEOUT_SECONDS);

        if (!result.success()) {
            throw new RuntimeException(
                    "Không lấy được TikTok channel_id từ video mẫu.\n"
                            + "Exit code: "
                            + result.exitCode()
                            + "\n"
                            + result.output()
            );
        }

        String channelId = firstValidTikTokChannelId(result.output());

        if (channelId.isBlank()) {
            throw new RuntimeException(
                    "yt-dlp không trả về channel_id hợp lệ từ video mẫu.\n"
                            + result.output()
            );
        }

        return channelId;
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
                            "Lệnh yt-dlp lấy TikTok channel_id chạy quá " + timeoutSeconds + " giây."
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

            return new CommandResult(false, -1, exception.getMessage());
        }
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

    private String firstValidTikTokChannelId(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }

        String[] lines = output.split("\\R");

        for (String line : lines) {
            if (line == null) {
                continue;
            }

            String cleanLine = line.trim();

            if (isValidTikTokChannelId(cleanLine)) {
                return cleanLine;
            }
        }

        return "";
    }

    private String buildSampleVideoUrl(String sourceUrl, VideoCandidate sampleVideo) {
        if (sampleVideo == null) {
            return "";
        }

        if (sampleVideo.getUrl() != null && !sampleVideo.getUrl().isBlank()) {
            return sampleVideo.getUrl().trim();
        }

        String videoId = sampleVideo.getVideoId();

        if (videoId == null || videoId.isBlank()) {
            return "";
        }

        String username = extractTikTokUsername(sourceUrl);

        if (username.isBlank()) {
            return "";
        }

        return "https://www.tiktok.com/@" + username + "/video/" + videoId.trim();
    }

    private String extractTikTokUsername(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return "";
        }

        String cleanSourceUrl = sourceUrl.trim();
        int atIndex = cleanSourceUrl.indexOf("@");

        if (atIndex < 0) {
            return "";
        }

        String usernamePart = cleanSourceUrl.substring(atIndex + 1);
        int slashIndex = usernamePart.indexOf("/");

        if (slashIndex >= 0) {
            usernamePart = usernamePart.substring(0, slashIndex);
        }

        int questionIndex = usernamePart.indexOf("?");

        if (questionIndex >= 0) {
            usernamePart = usernamePart.substring(0, questionIndex);
        }

        return usernamePart.trim();
    }

    private boolean isTikTokSource(String sourceType, String sourceUrl) {
        if (sourceType != null && sourceType.equalsIgnoreCase("TIKTOK")) {
            return true;
        }

        if (sourceUrl == null) {
            return false;
        }

        String lowerUrl = sourceUrl.toLowerCase();

        return lowerUrl.contains("tiktok.com")
                || lowerUrl.contains("vm.tiktok.com")
                || lowerUrl.contains("vt.tiktok.com")
                || lowerUrl.startsWith("tiktokuser:");
    }

    private boolean isResolvedTikTokUserUrl(String sourceUrl) {
        if (sourceUrl == null) {
            return false;
        }

        return sourceUrl.trim().toLowerCase().startsWith("tiktokuser:");
    }

    private boolean isValidTikTokChannelId(String channelId) {
        if (channelId == null || channelId.isBlank()) {
            return false;
        }

        String cleanChannelId = channelId.trim();

        if (cleanChannelId.equalsIgnoreCase("NA")
                || cleanChannelId.equalsIgnoreCase("null")
                || cleanChannelId.equalsIgnoreCase("none")
                || cleanChannelId.contains("%(")
                || cleanChannelId.contains(")s")) {
            return false;
        }

        return cleanChannelId.length() >= 12;
    }

    private String safeTrim(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }

    private record CommandResult(boolean success, int exitCode, String output) {
    }

    public record TikTokResolveResult(
            String channelId,
            String resolvedSourceUrl,
            String sampleVideoUrl
    ) {
    }
}
