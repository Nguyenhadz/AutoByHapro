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
        List<VideoDownloadItemResult> results = new ArrayList<>();

        if (videos == null || videos.isEmpty()) {
            return results;
        }

        if (folders == null || folders.isEmpty()) {
            throw new RuntimeException("Chưa có folder batch để tải video.");
        }

        for (int index = 0; index < videos.size(); index++) {
            VideoCandidate video = videos.get(index);

            int folderIndex = index / VIDEO_PER_FOLDER_BATCH;

            if (folderIndex >= folders.size()) {
                folderIndex = folders.size() - 1;
            }

            int indexInFolder = (index % VIDEO_PER_FOLDER_BATCH) + 1;
            VideoBatchFolder folder = folders.get(folderIndex);

            System.out.println();
            System.out.println("Đang tải video " + (index + 1) + "/" + videos.size());
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
                System.out.println("Tải thành công: " + itemResult.getFilePath());
            } else {
                System.out.println("Tải thất bại: " + itemResult.getMessage());
            }
        }

        return results;
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
                return fail(
                        video,
                        folder,
                        "yt-dlp tải lỗi. Exit code: "
                                + commandResult.exitCode()
                                + "\n"
                                + commandResult.output()
                );
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

    private String buildWindowsCompatibleMp4FormatSelector() {
        return String.join("/",
                // YouTube: ưu tiên video H.264/AVC1 mp4 + audio AAC/M4A
                "bestvideo[ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a][acodec^=mp4a]",

                // YouTube fallback: video H.264/AVC1 mp4 + audio m4a
                "bestvideo[ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a]",

                // Một số nền tảng ghi codec là h264 thay vì avc1
                "bestvideo[ext=mp4][vcodec^=h264]+bestaudio[ext=m4a][acodec^=mp4a]",
                "bestvideo[ext=mp4][vcodec^=h264]+bestaudio[ext=m4a]",

                // File mp4 có sẵn cả video + audio, ưu tiên H.264 + AAC
                "best[ext=mp4][vcodec^=avc1][acodec^=mp4a]",
                "best[ext=mp4][vcodec^=h264][acodec^=mp4a]",

                // Fallback cuối: vẫn chỉ lấy mp4 H.264, không lấy webm/av1/vp9
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
        return String.join("/",
                // TikTok: ưu tiên đúng nhóm format H.264 có sẵn cả hình và tiếng.
                // Tránh "best[ext=mp4]" vì có thể chọn bytevc1/HEVC bị mất âm thanh.
                "best[format_id^=h264_][acodec!=none]",

                // Fallback khi yt-dlp ghi codec là h264 hoặc avc1 thay vì dùng prefix format_id.
                "best[ext=mp4][vcodec^=h264][acodec!=none]",
                "best[ext=mp4][vcodec^=avc1][acodec!=none]",

                // Một số video TikTok có format H.264 nhưng metadata audio không khai báo đầy đủ.
                // Vẫn ưu tiên H.264; không fallback sang bytevc1 để tránh tải file chỉ có hình.
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

        // Ép UTF-8 để log/output không làm hỏng tiếng Việt.
        command.add("--encoding");
        command.add("utf-8");

        // Ưu tiên metadata tiếng Việt khi tải YouTube.
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
        if (video.getUrl() != null && !video.getUrl().isBlank()) {
            return video.getUrl();
        }

        if ("YOUTUBE".equalsIgnoreCase(target.getSourceType())) {
            return "https://www.youtube.com/watch?v=" + video.getVideoId();
        }

        return video.getVideoId();
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

        // Giữ title gốc từ video, không tự đổi chữ hoa/thường ở bước raw.
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

    private record CommandResult(
            boolean success,
            int exitCode,
            String output
    ) {
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
}