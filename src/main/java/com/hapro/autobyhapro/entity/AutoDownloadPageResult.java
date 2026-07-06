package com.hapro.autobyhapro.entity;

import java.util.List;

public class AutoDownloadPageResult {

    private final DownloadTarget target;
    private final int requestedCount;
    private final int scannedCount;
    private final int skippedExistingCount;
    private final int foundNewCount;
    private final int downloadedCount;
    private final int failedCount;
    private final Long downloadBatchId;
    private final List<VideoCandidate> videosToDownload;
    private final List<VideoBatchFolder> videoBatchFolders;
    private final List<VideoDownloadItemResult> downloadItemResults;
    private final String status;
    private final String message;

    public AutoDownloadPageResult(
            DownloadTarget target,
            int requestedCount,
            int scannedCount,
            int skippedExistingCount,
            int foundNewCount,
            int downloadedCount,
            int failedCount,
            Long downloadBatchId,
            List<VideoCandidate> videosToDownload,
            List<VideoBatchFolder> videoBatchFolders,
            List<VideoDownloadItemResult> downloadItemResults,
            String status,
            String message
    ) {
        this.target = target;
        this.requestedCount = requestedCount;
        this.scannedCount = scannedCount;
        this.skippedExistingCount = skippedExistingCount;
        this.foundNewCount = foundNewCount;
        this.downloadedCount = downloadedCount;
        this.failedCount = failedCount;
        this.downloadBatchId = downloadBatchId;
        this.videosToDownload = videosToDownload;
        this.videoBatchFolders = videoBatchFolders;
        this.downloadItemResults = downloadItemResults;
        this.status = status;
        this.message = message;
    }

    public DownloadTarget getTarget() {
        return target;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public int getScannedCount() {
        return scannedCount;
    }

    public int getSkippedExistingCount() {
        return skippedExistingCount;
    }

    public int getFoundNewCount() {
        return foundNewCount;
    }

    public int getDownloadedCount() {
        return downloadedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public Long getDownloadBatchId() {
        return downloadBatchId;
    }

    public List<VideoCandidate> getVideosToDownload() {
        return videosToDownload;
    }

    public List<VideoBatchFolder> getVideoBatchFolders() {
        return videoBatchFolders;
    }

    public List<VideoDownloadItemResult> getDownloadItemResults() {
        return downloadItemResults;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}