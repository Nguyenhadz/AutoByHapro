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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AutoDownloadJobService {

    private static final int VIDEO_PER_FOLDER_BATCH = 6;

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
        System.out.println();
        System.out.println("====================================");
        System.out.println("Đang xử lý page:");
        System.out.println(target.getFanpageCode() + " - " + target.getFanpageName());
        System.out.println("Source: " + target.getSourceCode() + " - " + target.getSourceName());
        System.out.println("Yêu cầu: " + requestedCount + " video");
        System.out.println("====================================");

        Long downloadBatchId = null;
        List<VideoBatchFolder> folders = List.of();
        List<VideoCandidate> videosToDownload = List.of();
        List<VideoDownloadItemResult> downloadResults = List.of();

        try {
            Source source = toSource(target);

            int scanRequestCount = buildInitialScanRequestCount(
                    target,
                    requestedCount
            );

            int maxScanRequestCount = buildMaxScanRequestCount(
                    target,
                    requestedCount
            );

            SourceScanResult scanResult = null;

            while (true) {
                scanResult = sourceScannerService.findNewVideos(
                        source,
                        scanRequestCount
                );

                videosToDownload = selectDownloadableVideos(
                        target,
                        scanResult.getNewVideos(),
                        scanRequestCount
                );

                if (videosToDownload.size() >= requestedCount) {
                    break;
                }

                if (scanRequestCount >= maxScanRequestCount) {
                    break;
                }

                int nextScanRequestCount = Math.min(
                        scanRequestCount * 2,
                        maxScanRequestCount
                );

                if (nextScanRequestCount == scanRequestCount) {
                    break;
                }

                System.out.println(
                        "Chưa đủ video ứng viên. Tăng số bài quét từ "
                                + scanRequestCount
                                + " lên "
                                + nextScanRequestCount
                                + "..."
                );

                scanRequestCount = nextScanRequestCount;
            }

            if (scanResult == null) {
                return new AutoDownloadPageResult(
                        target,
                        requestedCount,
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
                        requestedCount,
                        scanResult.getScannedCount(),
                        scanResult.getSkippedExistingCount(),
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

            downloadBatchId = downloadPlanRepository.createDownloadBatch(
                    target.getFanpageId(),
                    target.getSourceId(),
                    requestedCount,
                    threadCount
            );

            folders = createVideoBatchFolders(
                    target,
                    downloadBatchId,
                    requestedCount
            );

            downloadResults = videoDownloadService.downloadVideos(
                    target,
                    videosToDownload,
                    folders,
                    requestedCount
            );

            int downloadedCount = countSuccess(downloadResults);
            int failedCount = downloadResults.size() - downloadedCount;

            updateBatchStatuses(
                    downloadBatchId,
                    requestedCount,
                    folders,
                    downloadResults
            );

            String status;

            if (downloadedCount == 0) {
                status = "DOWNLOAD_FAILED";
            } else if (downloadedCount < requestedCount) {
                status = "DOWNLOADED_PARTIAL";
            } else {
                status = "DOWNLOADED_FULL";
            }

            String message = "Yêu cầu "
                    + requestedCount
                    + " video, chuẩn bị "
                    + videosToDownload.size()
                    + " video ứng viên, tải thành công "
                    + downloadedCount
                    + " video.";

            return new AutoDownloadPageResult(
                    target,
                    requestedCount,
                    scanResult.getScannedCount(),
                    scanResult.getSkippedExistingCount(),
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
                    requestedCount,
                    0,
                    0,
                    videosToDownload.size(),
                    countSuccess(downloadResults),
                    Math.max(0, videosToDownload.size() - countSuccess(downloadResults)),
                    downloadBatchId,
                    videosToDownload,
                    folders,
                    downloadResults,
                    "FAILED",
                    exception.getMessage()
            );
        }
    }

    private String buildCandidateUrl(DownloadTarget target, VideoCandidate video) {
        if (video.getUrl() != null && !video.getUrl().isBlank()) {
            return video.getUrl();
        }

        if ("YOUTUBE".equalsIgnoreCase(target.getSourceType())) {
            return "https://www.youtube.com/watch?v=" + video.getVideoId();
        }

        return video.getVideoId();
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
                || lowerUrl.contains("vt.tiktok.com");
    }

    private int buildInitialScanRequestCount(DownloadTarget target, int requestedCount) {
        int safeRequestedCount = Math.max(1, requestedCount);

        if (isTikTokTarget(target)) {
            return Math.min(
                    Math.max(safeRequestedCount * 5, 30),
                    100
            );
        }

        /*
         * YouTube cũng lấy dư ứng viên.
         * Nếu vài video đầu là members-only/private/unavailable,
         * VideoDownloadService sẽ ghi chúng vào DB rồi thử video tiếp theo trong cùng lượt tải.
         */
        return Math.min(
                Math.max(safeRequestedCount * 3, safeRequestedCount + 12),
                80
        );
    }

    private int buildMaxScanRequestCount(DownloadTarget target, int requestedCount) {
        int safeRequestedCount = Math.max(1, requestedCount);

        if (isTikTokTarget(target)) {
            return Math.max(1000, safeRequestedCount * 20);
        }

        return Math.max(100, safeRequestedCount * 10);
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

            Path rawBatchFolder = pageRawFolder.resolve(batchCode);
            Path editedBatchFolder = pageEditedFolder.resolve(batchCode);

            createDirectory(rawBatchFolder);
            createDirectory(editedBatchFolder);

            long videoBatchId = downloadPlanRepository.createVideoBatch(
                    batchCode,
                    target.getFanpageId(),
                    target.getSourceId(),
                    downloadBatchId,
                    batchIndex,
                    batchVideoCount,
                    rawBatchFolder.toAbsolutePath().toString(),
                    editedBatchFolder.toAbsolutePath().toString()
            );

            VideoBatchFolder videoBatchFolder = new VideoBatchFolder(
                    videoBatchId,
                    batchCode,
                    batchVideoCount,
                    rawBatchFolder,
                    editedBatchFolder
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
