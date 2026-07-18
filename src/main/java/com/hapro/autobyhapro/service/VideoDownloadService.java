package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.DownloadTarget;
import com.hapro.autobyhapro.entity.VideoBatchFolder;
import com.hapro.autobyhapro.entity.VideoCandidate;
import com.hapro.autobyhapro.entity.VideoDownloadItemResult;
import com.hapro.autobyhapro.repository.VideoRepository;
import com.hapro.autobyhapro.util.FileNameUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VideoDownloadService {

    private static final int VIDEO_PER_FOLDER_BATCH = 6;
    private static final int DOWNLOAD_TIMEOUT_SECONDS = 900;

    private final VideoRepository videoRepository = new VideoRepository();

    public List<VideoDownloadItemResult> downloadVideos(
            DownloadTarget target,
            List<VideoCandidate> videos,
            List<VideoBatchFolder> folders
    ) {
        int requestedSuccessCount = videos == null ? 0 : videos.size();

        return downloadVideos(
                target,
                videos,
                folders,
                requestedSuccessCount
        );
    }

    public List<VideoDownloadItemResult> downloadVideos(
            DownloadTarget target,
            List<VideoCandidate> videos,
            List<VideoBatchFolder> folders,
            int requestedSuccessCount
    ) {
        List<VideoDownloadItemResult> results = new ArrayList<>();

        if (videos == null || videos.isEmpty()) {
            return results;
        }

        if (folders == null || folders.isEmpty()) {
            throw new RuntimeException("Chưa có folder batch để tải video.");
        }

        int folderCapacity = countFolderCapacity(folders);
        int targetSuccessCount = requestedSuccessCount;

        if (targetSuccessCount <= 0) {
            targetSuccessCount = folderCapacity;
        }

        if (folderCapacity > 0) {
            targetSuccessCount = Math.min(targetSuccessCount, folderCapacity);
        }

        int successCount = 0;
        int attemptCount = 0;

        for (VideoCandidate video : videos) {
            if (successCount >= targetSuccessCount) {
                break;
            }

            attemptCount++;

            int folderIndex = successCount / VIDEO_PER_FOLDER_BATCH;

            if (folderIndex >= folders.size()) {
                folderIndex = folders.size() - 1;
            }

            int indexInFolder = (successCount % VIDEO_PER_FOLDER_BATCH) + 1;
            VideoBatchFolder folder = folders.get(folderIndex);

            System.out.println();
            System.out.println("Đang tải video ứng viên " + attemptCount + "/" + videos.size());
            System.out.println("Đã tải thành công: " + successCount + "/" + targetSuccessCount);
            System.out.println("Batch: " + folder.getBatchCode());
            System.out.println("Video ID: " + video.getVideoId());
            System.out.println("Title: " + video.getTitle());

            VideoDownloadItemResult itemResult = downloadOneVideo(
                    target,
                    video,
                    folder,
                    indexInFolder
            );

            results.add(itemResult);

            if (itemResult.isSuccess()) {
                successCount++;
                System.out.println("Tải thành công: " + itemResult.getFilePath());
            } else {
                System.out.println("Tải thất bại: " + itemResult.getMessage());
            }
        }

        return results;
    }

    private int countFolderCapacity(List<VideoBatchFolder> folders) {
        int total = 0;

        if (folders == null) {
            return total;
        }

        for (VideoBatchFolder folder : folders) {
            if (folder == null) {
                continue;
            }

            total = total + Math.max(0, folder.getVideoCount());
        }

        return total;
    }

    private VideoDownloadItemResult downloadOneVideo(
            DownloadTarget target,
            VideoCandidate video,
            VideoBatchFolder folder,
            int indexInFolder
    ) {
        try {
            Path ytDlp = AppPaths.ytDlpFile();

            if (!Files.exists(ytDlp)) {
                return fail(video, folder, "Chưa có yt-dlp.exe tại: " + ytDlp.toAbsolutePath());
            }

            Files.createDirectories(folder.getRawFolderPath());

            String downloadUrl = buildDownloadUrl(target, video);

            if (downloadUrl == null || downloadUrl.isBlank()) {
                return fail(video, folder, "Không có URL để tải video.");
            }

            String baseFileName = buildBaseFileName(
                    video,
                    folder,
                    indexInFolder
            );

            Path outputTemplate = folder.getRawFolderPath().resolve(baseFileName + ".%(ext)s");

            List<String> command = buildDownloadCommand(
                    ytDlp,
                    downloadUrl,
                    outputTemplate
            );

            CommandResult commandResult = runCommand(command, DOWNLOAD_TIMEOUT_SECONDS);

            if (!commandResult.success()) {
                String fullMessage = "yt-dlp tải lỗi.\n"
                        + "Exit code: "
                        + commandResult.exitCode()
                        + "\n"
                        + commandResult.output();

                if (shouldRememberFailedVideoAsUnavailable(commandResult.output())) {
                    rememberSkippedUnavailableVideo(
                            target,
                            video,
                            downloadUrl
                    );

                    return fail(
                            video,
                            folder,
                            fullMessage
                                    + "\n\nĐã lưu video này vào DB với status SKIPPED_UNAVAILABLE."
                                    + "\nLần sau tool sẽ bỏ qua video này và quét video tiếp theo."
                    );
                }

                return fail(video, folder, fullMessage);
            }

            Path downloadedFile = findDownloadedFile(folder.getRawFolderPath(), baseFileName);

            if (downloadedFile == null) {
                return fail(
                        video,
                        folder,
                        "yt-dlp báo thành công nhưng không tìm thấy file đã tải."
                );
            }

            videoRepository.saveDownloadedVideo(
                    folder.getVideoBatchId(),
                    target.getSourceId(),
                    target.getFanpageId(),
                    video.getVideoId(),
                    video.getTitle(),
                    target.getSourceName(),
                    downloadUrl,
                    downloadedFile.toAbsolutePath().toString()
            );

            return new VideoDownloadItemResult(
                    video,
                    folder.getVideoBatchId(),
                    folder.getBatchCode(),
                    true,
                    downloadedFile.toAbsolutePath().toString(),
                    "OK"
            );

        } catch (Exception exception) {
            return fail(video, folder, exception.getMessage());
        }
    }

    private void rememberSkippedUnavailableVideo(
            DownloadTarget target,
            VideoCandidate video,
            String downloadUrl
    ) {
        videoRepository.saveSkippedUnavailableVideo(
                target.getSourceId(),
                target.getFanpageId(),
                video.getVideoId(),
                video.getTitle(),
                target.getSourceName(),
                downloadUrl
        );
    }

    private boolean shouldRememberFailedVideoAsUnavailable(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }

        String lowerOutput = output.toLowerCase();

        if (isProbablyTemporaryOrToolFailure(lowerOutput)) {
            return false;
        }

        return lowerOutput.contains("members-only")
                || lowerOutput.contains("members only")
                || lowerOutput.contains("member-only")
                || lowerOutput.contains("join this channel")
                || lowerOutput.contains("available to this channel's members")
                || lowerOutput.contains("available to members")
                || lowerOutput.contains("members have access")
                || lowerOutput.contains("subscribers only")
                || lowerOutput.contains("subscriber-only")
                || lowerOutput.contains("paid subscribers")
                || lowerOutput.contains("private video")
                || lowerOutput.contains("this video is private")
                || lowerOutput.contains("this account is private")
                || lowerOutput.contains("video unavailable")
                || lowerOutput.contains("this video is unavailable")
                || lowerOutput.contains("video is unavailable")
                || lowerOutput.contains("video is not available")
                || lowerOutput.contains("this video is not available")
                || lowerOutput.contains("post is unavailable")
                || lowerOutput.contains("couldn't find this video")
                || lowerOutput.contains("could not find this video")
                || lowerOutput.contains("has been removed")
                || lowerOutput.contains("has been deleted")
                || lowerOutput.contains("not made this video available")
                || lowerOutput.contains("blocked in your country")
                || lowerOutput.contains("only available to followers")
                || lowerOutput.contains("friends only");
    }

    private boolean isProbablyTemporaryOrToolFailure(String lowerOutput) {
        return lowerOutput.contains("unable to extract universal data")
                || lowerOutput.contains("please report this issue")
                || lowerOutput.contains("confirm you are on the latest version")
                || lowerOutput.contains("requested format is not available")
                || lowerOutput.contains("sign in to confirm you’re not a bot")
                || lowerOutput.contains("sign in to confirm you're not a bot")
                || lowerOutput.contains("not a bot")
                || lowerOutput.contains("too many requests")
                || lowerOutput.contains("http error 429")
                || lowerOutput.contains("429: too many requests")
                || lowerOutput.contains("timed out")
                || lowerOutput.contains("timeout")
                || lowerOutput.contains("network is unreachable")
                || lowerOutput.contains("connection reset")
                || lowerOutput.contains("connection aborted")
                || lowerOutput.contains("temporary failure")
                || lowerOutput.contains("temporarily unavailable")
                || lowerOutput.contains("unable to download webpage")
                || lowerOutput.contains("unsupported url");
    }

    private String buildWindowsCompatibleMp4FormatSelector() {
        return String.join(
                "/",
                "bestvideo[ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a][acodec^=mp4a]",
                "bestvideo[ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a]",
                "bestvideo[ext=mp4][vcodec^=h264]+bestaudio[ext=m4a][acodec^=mp4a]",
                "bestvideo[ext=mp4][vcodec^=h264]+bestaudio[ext=m4a]",
                "best[ext=mp4][vcodec^=avc1][acodec^=mp4a]",
                "best[ext=mp4][vcodec^=h264][acodec^=mp4a]",
                "best[ext=mp4][vcodec^=avc1]",
                "best[ext=mp4][vcodec^=h264]",
                "best[ext=mp4]"
        );
    }

    private String buildFormatSelector(String downloadUrl) {
        if (isYoutubeUrl(downloadUrl)) {
            return buildWindowsCompatibleMp4FormatSelector();
        }

        if (isTikTokUrl(downloadUrl)) {
            return buildTikTokFormatSelector();
        }

        return "best[ext=mp4]/bv*+ba/best";
    }

    private String buildTikTokFormatSelector() {
        return String.join(
                "/",
                "best[format_id^=h264_][acodec!=none]",
                "best[ext=mp4][vcodec^=h264][acodec!=none]",
                "best[ext=mp4][vcodec^=avc1][acodec!=none]",
                "best[format_id^=h264_]",
                "best[ext=mp4][vcodec^=h264]",
                "best[ext=mp4][vcodec^=avc1]"
        );
    }

    private boolean isTikTokUrl(String url) {
        if (url == null) {
            return false;
        }

        String lowerUrl = url.toLowerCase();

        return lowerUrl.contains("tiktok.com")
                || lowerUrl.contains("vm.tiktok.com")
                || lowerUrl.contains("vt.tiktok.com");
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

    private List<String> buildDownloadCommand(
            Path ytDlp,
            String downloadUrl,
            Path outputTemplate
    ) {
        List<String> command = new ArrayList<>();

        command.add(ytDlp.toAbsolutePath().toString());

        command.add("--encoding");
        command.add("utf-8");

        command.add("--extractor-args");
        command.add("youtube:lang=vi");

        command.add("--no-warnings");
        command.add("--no-playlist");
        command.add("--ignore-errors");
        command.add("--windows-filenames");

        addYoutubeSupportOptions(command, downloadUrl);

        command.add("--socket-timeout");
        command.add("20");

        command.add("--retries");
        command.add("2");

        command.add("-f");
        command.add(buildFormatSelector(downloadUrl));

        command.add("--merge-output-format");
        command.add("mp4");

        command.add("--remux-video");
        command.add("mp4");

        Path ffmpeg = AppPaths.ffmpegFile();

        if (Files.exists(ffmpeg)) {
            command.add("--ffmpeg-location");
            command.add(ffmpeg.getParent().toAbsolutePath().toString());
        }

        command.add("-o");
        command.add(outputTemplate.toAbsolutePath().toString());

        command.add(downloadUrl);

        return command;
    }

    private String buildDownloadUrl(DownloadTarget target, VideoCandidate video) {
        if (isTikTokTarget(target)) {
            return buildTikTokVideoUrlFromOriginalSource(target, video);
        }

        if (video.getUrl() != null && !video.getUrl().isBlank()) {
            return video.getUrl();
        }

        if ("YOUTUBE".equalsIgnoreCase(target.getSourceType())) {
            return "https://www.youtube.com/watch?v=" + video.getVideoId();
        }

        return video.getVideoId();
    }

    private String buildTikTokVideoUrlFromOriginalSource(
            DownloadTarget target,
            VideoCandidate video
    ) {
        String videoId = video == null ? "" : safeTrim(video.getVideoId());
        String originalSourceUrl = target == null ? "" : safeTrim(target.getSourceUrl());
        String username = extractTikTokUsername(originalSourceUrl);

        /*
         * resolved_source_url = tiktokuser:channel_id chỉ dùng để quét playlist.
         * Khi tải từng video, TikTok cần username thật ở dạng @username/video/id.
         * Không dùng @MS4w... vì MS4w... là secUid/channel_id, không phải username.
         */
        if (!username.isBlank() && !videoId.isBlank()) {
            return "https://www.tiktok.com/@" + username + "/video/" + videoId;
        }

        String candidateUrl = video == null ? "" : safeTrim(video.getUrl());

        if (!candidateUrl.isBlank()
                && !isTikTokVideoUrlUsingResolvedChannelId(candidateUrl)) {
            return candidateUrl;
        }

        if (!videoId.isBlank()) {
            return videoId;
        }

        return candidateUrl;
    }

    private boolean isTikTokTarget(DownloadTarget target) {
        if (target == null) {
            return false;
        }

        String sourceType = target.getSourceType();
        String sourceUrl = target.getSourceUrl();

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

    private boolean isTikTokVideoUrlUsingResolvedChannelId(String url) {
        String username = extractTikTokUsername(url);

        return looksLikeTikTokChannelId(username);
    }

    private boolean looksLikeTikTokChannelId(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String cleanValue = value.trim();

        return cleanValue.toLowerCase().startsWith("ms4wlj")
                || cleanValue.length() > 30
                || cleanValue.contains("-");
    }

    private String extractTikTokUsername(String urlOrUsername) {
        if (urlOrUsername == null || urlOrUsername.isBlank()) {
            return "";
        }

        String cleanText = urlOrUsername.trim();

        if (cleanText.toLowerCase().startsWith("tiktokuser:")) {
            return "";
        }

        int atIndex = cleanText.indexOf("@");

        if (atIndex >= 0) {
            String usernamePart = cleanText.substring(atIndex + 1);
            return cleanTikTokUsernamePart(usernamePart);
        }

        if (!cleanText.contains("/")
                && !cleanText.contains(":")
                && !cleanText.contains("?")) {
            return cleanTikTokUsernamePart(cleanText);
        }

        return "";
    }

    private String cleanTikTokUsernamePart(String usernamePart) {
        if (usernamePart == null || usernamePart.isBlank()) {
            return "";
        }

        String username = usernamePart.trim();

        int slashIndex = username.indexOf("/");
        if (slashIndex >= 0) {
            username = username.substring(0, slashIndex);
        }

        int questionIndex = username.indexOf("?");
        if (questionIndex >= 0) {
            username = username.substring(0, questionIndex);
        }

        return username.trim();
    }

    private String safeTrim(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }

    private String buildBaseFileName(
            VideoCandidate video,
            VideoBatchFolder folder,
            int indexInFolder
    ) {
        String safeVideoId = FileNameUtil.safeFileName(video.getVideoId(), 60);
        String compactBatchCode = compactBatchCode(folder.getBatchCode());

        String prefix = String.format(
                "VID_%s__B_%s__N_%03d",
                safeVideoId,
                compactBatchCode,
                indexInFolder
        );

        String safeTitle = FileNameUtil.safeFileName(video.getTitle(), 90);

        String baseName = prefix;

        if (!safeTitle.isBlank()
                && !"unknown".equalsIgnoreCase(safeTitle)
                && !"untitled".equalsIgnoreCase(safeTitle)) {
            baseName = baseName + "__TITLE_" + safeTitle;
        }

        return FileNameUtil.safeFileName(baseName, 180);
    }

    private String compactBatchCode(String batchCode) {
        if (batchCode == null || batchCode.isBlank()) {
            return "UNKNOWNBATCH";
        }

        return batchCode.replace("__", "");
    }

    private Path findDownloadedFile(Path folder, String baseFileName) {
        try {
            if (!Files.exists(folder)) {
                return null;
            }

            return Files.list(folder)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(baseFileName))
                    .max(Comparator.comparingLong(this::lastModifiedTime))
                    .orElse(null);

        } catch (Exception exception) {
            return null;
        }
    }

    private long lastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception exception) {
            return 0L;
        }
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
                            "Lệnh tải chạy quá " + timeoutSeconds + " giây nên tool đã tự dừng."
                    );
                }

                Thread.sleep(500);
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

    private VideoDownloadItemResult fail(
            VideoCandidate video,
            VideoBatchFolder folder,
            String message
    ) {
        return new VideoDownloadItemResult(
                video,
                folder.getVideoBatchId(),
                folder.getBatchCode(),
                false,
                null,
                message
        );
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

    private record CommandResult(
            boolean success,
            int exitCode,
            String output
    ) {
    }
}