package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.AutoDownloadJobResult;
import com.hapro.autobyhapro.entity.AutoDownloadPageResult;
import com.hapro.autobyhapro.entity.DownloadTarget;
import com.hapro.autobyhapro.entity.Source;
import com.hapro.autobyhapro.entity.SourceScanResult;
import com.hapro.autobyhapro.entity.VideoBatchFolder;
import com.hapro.autobyhapro.entity.VideoCandidate;
import com.hapro.autobyhapro.entity.VideoDownloadItemResult;
import com.hapro.autobyhapro.repository.DownloadPlanRepository;
import com.hapro.autobyhapro.repository.SourceContentCacheRepository;
import com.hapro.autobyhapro.util.FileNameUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AutoDownloadJobService {

    private static final int VIDEO_PER_FOLDER_BATCH = 6;
    private static final int FOLLOW_UP_SCAN_STEP = 10;

    private final SourceScannerService sourceScannerService = new SourceScannerService();
    private final DownloadPlanRepository downloadPlanRepository = new DownloadPlanRepository();
    private final VideoDownloadService videoDownloadService = new VideoDownloadService();
    private final YtDlpService ytDlpService = new YtDlpService();
    private final SourceContentCacheRepository sourceContentCacheRepository = new SourceContentCacheRepository();

    public AutoDownloadJobResult runJob(
            List<DownloadTarget> selectedTargets,
            Map<Long, Integer> requestedCountByFanpageId,
            int threadCount
    ) {
        if (selectedTargets == null || selectedTargets.isEmpty()) {
            return new AutoDownloadJobResult(1, new ArrayList<>());
        }

        int safeThreadCount = normalizeThreadCount(threadCount);
        int actualThreadCount = Math.min(safeThreadCount, selectedTargets.size());

        if (actualThreadCount <= 1) {
            List<AutoDownloadPageResult> pageResults = new ArrayList<>();

            for (DownloadTarget target : selectedTargets) {
                int requestedCount = requestedCountByFanpageId.getOrDefault(
                        target.getFanpageId(),
                        target.getDefaultVideoCount()
                );

                AutoDownloadPageResult pageResult = runOnePage(
                        target,
                        requestedCount,
                        actualThreadCount
                );

                pageResults.add(pageResult);
            }

            return new AutoDownloadJobResult(1, pageResults);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(actualThreadCount);
        List<Future<AutoDownloadPageResult>> futures = new ArrayList<>();

        try {
            for (DownloadTarget target : selectedTargets) {
                int requestedCount = requestedCountByFanpageId.getOrDefault(
                        target.getFanpageId(),
                        target.getDefaultVideoCount()
                );

                Future<AutoDownloadPageResult> future = executorService.submit(
                        () -> runOnePage(target, requestedCount, actualThreadCount)
                );

                futures.add(future);
            }

            List<AutoDownloadPageResult> pageResults = new ArrayList<>();

            for (Future<AutoDownloadPageResult> future : futures) {
                pageResults.add(future.get());
            }

            return new AutoDownloadJobResult(actualThreadCount, pageResults);

        } catch (Exception exception) {
            throw new RuntimeException("Tải song song bị lỗi.", exception);
        } finally {
            executorService.shutdownNow();
        }
    }

    private int normalizeThreadCount(int threadCount) {
        if (threadCount < 1) {
            return 1;
        }

        return threadCount;
    }

    private AutoDownloadPageResult runOnePage(
            DownloadTarget target,
            int requestedCount,
            int threadCount
    ) {
        int safeRequestedCount = Math.max(1, requestedCount);

        System.out.println();
        System.out.println("====================================");
        System.out.println("Đang xử lý page:");
        System.out.println(target.getFanpageCode() + " - " + target.getFanpageName());
        System.out.println("Source: " + target.getSourceCode() + " - " + target.getSourceName());
        System.out.println("Yêu cầu: " + safeRequestedCount + " video");
        System.out.println("====================================");

        Long downloadBatchId = null;
        List<VideoBatchFolder> folders = List.of();
        List<VideoCandidate> videosToDownload = new ArrayList<>();
        List<VideoDownloadItemResult> downloadResults = new ArrayList<>();
        SourceScanResult latestScanResult = null;

        try {
            Source source = toSource(target);

            int currentScanLimit = buildInitialScanRequestCount(safeRequestedCount);
            int maxScanLimit = buildMaxScanRequestCount(target, safeRequestedCount);
            int lastScannedCount = -1;
            boolean firstScan = true;

            Set<String> attemptedVideoIds = new LinkedHashSet<>();

            while (countSuccess(downloadResults) < safeRequestedCount
                    && currentScanLimit <= maxScanLimit) {

                int beforeSuccessCount = countSuccess(downloadResults);

                if (firstScan) {
                    System.out.println(
                            "Quét lần đầu: "
                                    + safeRequestedCount
                                    + " x 2 = "
                                    + currentScanLimit
                                    + " video."
                    );
                } else {
                    System.out.println(
                            "Chưa tải đủ "
                                    + safeRequestedCount
                                    + " video. Quét tiếp thêm "
                                    + FOLLOW_UP_SCAN_STEP
                                    + " video, tổng số video quét tối đa: "
                                    + currentScanLimit
                                    + "."
                    );
                }

                latestScanResult = sourceScannerService.findNewVideos(
                        source,
                        currentScanLimit
                );

                if (latestScanResult.getNewVideos() == null
                        || latestScanResult.getNewVideos().isEmpty()) {
                    break;
                }

                List<VideoCandidate> unattemptedCandidates = filterUnattemptedVideos(
                        latestScanResult.getNewVideos(),
                        attemptedVideoIds
                );

                if (unattemptedCandidates.isEmpty()) {
                    if (latestScanResult.getScannedCount() == lastScannedCount) {
                        break;
                    }

                    lastScannedCount = latestScanResult.getScannedCount();
                    currentScanLimit = nextScanLimit(currentScanLimit, maxScanLimit);
                    firstScan = false;
                    continue;
                }

                int roundCandidateLimit = firstScan
                        ? currentScanLimit
                        : FOLLOW_UP_SCAN_STEP;

                List<VideoCandidate> roundCandidates = selectDownloadableVideos(
                        target,
                        unattemptedCandidates,
                        roundCandidateLimit
                );

                if (roundCandidates.isEmpty()) {
                    if (latestScanResult.getScannedCount() == lastScannedCount) {
                        break;
                    }

                    lastScannedCount = latestScanResult.getScannedCount();
                    currentScanLimit = nextScanLimit(currentScanLimit, maxScanLimit);
                    firstScan = false;
                    continue;
                }

                for (VideoCandidate candidate : roundCandidates) {
                    attemptedVideoIds.add(candidate.getVideoId());
                }

                if (downloadBatchId == null) {
                    downloadBatchId = downloadPlanRepository.createDownloadBatch(
                            target.getFanpageId(),
                            target.getSourceId(),
                            safeRequestedCount,
                            threadCount
                    );

                    folders = createVideoBatchFolders(
                            target,
                            downloadBatchId,
                            safeRequestedCount
                    );
                }

                videosToDownload.addAll(roundCandidates);

                List<VideoDownloadItemResult> roundDownloadResults =
                        videoDownloadService.downloadVideos(
                                target,
                                roundCandidates,
                                folders,
                                safeRequestedCount,
                                beforeSuccessCount
                        );

                downloadResults.addAll(roundDownloadResults);

                if (countSuccess(downloadResults) >= safeRequestedCount) {
                    break;
                }

                if (latestScanResult.getScannedCount() == lastScannedCount
                        || latestScanResult.getScannedCount() < currentScanLimit) {
                    break;
                }

                lastScannedCount = latestScanResult.getScannedCount();
                currentScanLimit = nextScanLimit(currentScanLimit, maxScanLimit);
                firstScan = false;
            }

            if (latestScanResult == null) {
                return new AutoDownloadPageResult(
                        target,
                        safeRequestedCount,
                        0,
                        0,
                        0,
                        0,
                        0,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        "NO_VIDEO",
                        "Không đọc được source."
                );
            }

            if (videosToDownload.isEmpty()) {
                return new AutoDownloadPageResult(
                        target,
                        safeRequestedCount,
                        latestScanResult.getScannedCount(),
                        latestScanResult.getSkippedExistingCount(),
                        0,
                        0,
                        0,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        "NO_VIDEO",
                        "Không tìm được video mới từ source này."
                );
            }

            int downloadedCount = countSuccess(downloadResults);
            int failedCount = downloadResults.size() - downloadedCount;

            updateBatchStatuses(
                    downloadBatchId,
                    safeRequestedCount,
                    folders,
                    downloadResults
            );

            String status;

            if (downloadedCount == 0) {
                status = "DOWNLOAD_FAILED";
            } else if (downloadedCount < safeRequestedCount) {
                status = "DOWNLOADED_PARTIAL";
            } else {
                status = "DOWNLOADED_FULL";
            }

            String message = "Yêu cầu "
                    + safeRequestedCount
                    + " video, chuẩn bị "
                    + videosToDownload.size()
                    + " video ứng viên, tải thành công "
                    + downloadedCount
                    + " video.";

            return new AutoDownloadPageResult(
                    target,
                    safeRequestedCount,
                    latestScanResult.getScannedCount(),
                    latestScanResult.getSkippedExistingCount(),
                    videosToDownload.size(),
                    downloadedCount,
                    failedCount,
                    downloadBatchId,
                    videosToDownload,
                    folders,
                    downloadResults,
                    status,
                    message
            );

        } catch (Exception exception) {
            if (downloadBatchId != null) {
                downloadPlanRepository.updateDownloadBatchResult(
                        downloadBatchId,
                        countSuccess(downloadResults),
                        "FAILED"
                );
            }

            return new AutoDownloadPageResult(
                    target,
                    safeRequestedCount,
                    latestScanResult == null ? 0 : latestScanResult.getScannedCount(),
                    latestScanResult == null ? 0 : latestScanResult.getSkippedExistingCount(),
                    videosToDownload.size(),
                    countSuccess(downloadResults),
                    Math.max(0, downloadResults.size() - countSuccess(downloadResults)),
                    downloadBatchId,
                    videosToDownload,
                    folders,
                    downloadResults,
                    "FAILED",
                    exception.getMessage()
            );
        }
    }

    private List<VideoCandidate> filterUnattemptedVideos(
            List<VideoCandidate> candidates,
            Set<String> attemptedVideoIds
    ) {
        List<VideoCandidate> result = new ArrayList<>();

        if (candidates == null || candidates.isEmpty()) {
            return result;
        }

        for (VideoCandidate candidate : candidates) {
            if (candidate == null
                    || candidate.getVideoId() == null
                    || candidate.getVideoId().isBlank()) {
                continue;
            }

            if (attemptedVideoIds.contains(candidate.getVideoId())) {
                continue;
            }

            result.add(candidate);
        }

        return result;
    }

    private int buildInitialScanRequestCount(int requestedCount) {
        return Math.max(1, requestedCount * 2);
    }

    private int nextScanLimit(int currentScanLimit, int maxScanLimit) {
        if (currentScanLimit >= maxScanLimit) {
            return maxScanLimit + 1;
        }

        return Math.min(
                currentScanLimit + FOLLOW_UP_SCAN_STEP,
                maxScanLimit
        );
    }

    private String buildCandidateUrl(DownloadTarget target, VideoCandidate video) {
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

    private int buildMaxScanRequestCount(DownloadTarget target, int requestedCount) {
        int safeRequestedCount = Math.max(1, requestedCount);

        if (isTikTokTarget(target)) {
            return Math.max(500, safeRequestedCount * 30);
        }

        return Math.max(200, safeRequestedCount * 20);
    }

    private List<VideoCandidate> selectDownloadableVideos(
            DownloadTarget target,
            List<VideoCandidate> candidates,
            int requestedCount
    ) {
        List<VideoCandidate> selectedVideos = new ArrayList<>();

        if (candidates == null || candidates.isEmpty()) {
            return selectedVideos;
        }

        int safeRequestedCount = Math.max(1, requestedCount);

        if (!isTikTokTarget(target)) {
            for (VideoCandidate candidate : candidates) {
                if (selectedVideos.size() >= safeRequestedCount) {
                    break;
                }

                selectedVideos.add(candidate);
            }

            return selectedVideos;
        }

        System.out.println("Đang lọc TikTok bằng cache DB: chỉ lấy VIDEO, bỏ PHOTO/list ảnh...");

        for (VideoCandidate candidate : candidates) {
            if (selectedVideos.size() >= safeRequestedCount) {
                break;
            }

            if (candidate == null
                    || candidate.getVideoId() == null
                    || candidate.getVideoId().isBlank()) {
                continue;
            }

            String cachedContentType = sourceContentCacheRepository.findStableContentType(
                    target.getSourceId(),
                    candidate.getVideoId()
            );

            if ("VIDEO".equalsIgnoreCase(cachedContentType)) {
                System.out.println("Cache HIT VIDEO: " + candidate.getVideoId());
                selectedVideos.add(candidate);
                continue;
            }

            if ("PHOTO".equalsIgnoreCase(cachedContentType)
                    || "AUDIO_ONLY".equalsIgnoreCase(cachedContentType)) {
                System.out.println("Cache HIT bỏ qua " + cachedContentType + ": " + candidate.getVideoId());
                continue;
            }

            String videoUrl = buildCandidateUrl(target, candidate);

            if (videoUrl == null || videoUrl.isBlank()) {
                System.out.println("Bỏ qua TikTok candidate vì URL trống: " + candidate.getVideoId());
                continue;
            }

            System.out.println("Cache MISS, kiểm tra TikTok: " + videoUrl);

            String contentType = ytDlpService.classifyTikTokContentUrl(videoUrl);
            String checkStatus = "ERROR".equalsIgnoreCase(contentType) ? "ERROR" : "OK";

            sourceContentCacheRepository.saveCheckResult(
                    target.getSourceId(),
                    target.getSourceType(),
                    candidate,
                    contentType,
                    checkStatus,
                    "Auto check TikTok content type before download"
            );

            if ("VIDEO".equalsIgnoreCase(contentType)) {
                System.out.println("Đã xác định VIDEO, thêm vào danh sách tải: " + candidate.getVideoId());
                selectedVideos.add(candidate);
                continue;
            }

            System.out.println("Bỏ qua TikTok " + contentType + ": " + candidate.getVideoId());
        }

        System.out.println(
                "TikTok lọc xong: chọn được "
                        + selectedVideos.size()
                        + "/"
                        + safeRequestedCount
                        + " video thật."
        );

        return selectedVideos;
    }

    private List<VideoBatchFolder> createVideoBatchFolders(
            DownloadTarget target,
            long downloadBatchId,
            int actualVideoCount
    ) {
        String pageFolderName = FileNameUtil.safeFolderName(
                target.getFanpageCode()
                        + "_"
                        + target.getFanpageName(),
                80
        );

        Path pageRawFolder = AppPaths.RAW_DIR.resolve(pageFolderName);
        Path pageEditedFolder = AppPaths.EDITED_DIR.resolve(pageFolderName);

        createDirectory(pageRawFolder);
        createDirectory(pageEditedFolder);

        int totalBatchCount = (int) Math.ceil(actualVideoCount / (double) VIDEO_PER_FOLDER_BATCH);
        int remainingVideoCount = actualVideoCount;

        List<VideoBatchFolder> videoBatchFolders = new ArrayList<>();

        for (int batchIndex = 1; batchIndex <= totalBatchCount; batchIndex++) {
            int batchVideoCount = Math.min(
                    VIDEO_PER_FOLDER_BATCH,
                    remainingVideoCount
            );

            String batchCode = String.format(
                    "%s__D%06d__B%03d",
                    target.getFanpageCode(),
                    downloadBatchId,
                    batchIndex
            );

            long videoBatchId = downloadPlanRepository.createVideoBatch(
                    batchCode,
                    target.getFanpageId(),
                    target.getSourceId(),
                    downloadBatchId,
                    batchIndex,
                    batchVideoCount,
                    pageRawFolder.toAbsolutePath().toString(),
                    pageEditedFolder.toAbsolutePath().toString()
            );

            VideoBatchFolder videoBatchFolder = new VideoBatchFolder(
                    videoBatchId,
                    batchCode,
                    batchVideoCount,
                    pageRawFolder,
                    pageEditedFolder
            );

            videoBatchFolders.add(videoBatchFolder);

            remainingVideoCount = remainingVideoCount - batchVideoCount;
        }

        return videoBatchFolders;
    }

    private void updateBatchStatuses(
            Long downloadBatchId,
            int requestedCount,
            List<VideoBatchFolder> folders,
            List<VideoDownloadItemResult> downloadResults
    ) {
        if (downloadBatchId == null) {
            return;
        }

        int downloadedCount = countSuccess(downloadResults);

        String downloadBatchStatus;

        if (downloadedCount == 0) {
            downloadBatchStatus = "FAILED";
        } else if (downloadedCount < requestedCount) {
            downloadBatchStatus = "PARTIAL";
        } else {
            downloadBatchStatus = "SUCCESS";
        }

        downloadPlanRepository.updateDownloadBatchResult(
                downloadBatchId,
                downloadedCount,
                downloadBatchStatus
        );

        Map<Long, Integer> successCountByVideoBatchId = new LinkedHashMap<>();

        for (VideoDownloadItemResult result : downloadResults) {
            if (!result.isSuccess()) {
                continue;
            }

            successCountByVideoBatchId.put(
                    result.getVideoBatchId(),
                    successCountByVideoBatchId.getOrDefault(result.getVideoBatchId(), 0) + 1
            );
        }

        for (VideoBatchFolder folder : folders) {
            int successCount = successCountByVideoBatchId.getOrDefault(
                    folder.getVideoBatchId(),
                    0
            );

            String status;

            if (successCount == 0) {
                status = "FAILED";
            } else if (successCount < folder.getVideoCount()) {
                status = "PARTIAL";
            } else {
                status = "DOWNLOADED";
            }

            downloadPlanRepository.updateVideoBatchResult(
                    folder.getVideoBatchId(),
                    successCount,
                    status
            );
        }
    }

    private int countSuccess(List<VideoDownloadItemResult> results) {
        int count = 0;

        if (results == null) {
            return 0;
        }

        for (VideoDownloadItemResult result : results) {
            if (result.isSuccess()) {
                count++;
            }
        }

        return count;
    }

    private Source toSource(DownloadTarget target) {
        return new Source(
                target.getSourceId(),
                target.getFanpageId(),
                target.getFanpageCode(),
                target.getFanpageName(),
                target.getSourceCode(),
                target.getSourceName(),
                target.getSourceType(),
                target.getSourceUrl(),
                target.getSourceName(),
                true,
                ""
        );
    }

    private void createDirectory(Path folder) {
        try {
            Files.createDirectories(folder);
        } catch (IOException exception) {
            throw new RuntimeException("Không thể tạo folder: " + folder.toAbsolutePath(), exception);
        }
    }
}
