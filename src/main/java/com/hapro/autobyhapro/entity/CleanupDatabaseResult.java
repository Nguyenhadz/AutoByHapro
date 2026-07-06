package com.hapro.autobyhapro.entity;

public class CleanupDatabaseResult {

    private final int videoFilesDeletedCount;
    private final int videosUpdatedCount;
    private final String newBatchStatus;
    private final String newDownloadBatchStatus;

    public CleanupDatabaseResult(
            int videoFilesDeletedCount,
            int videosUpdatedCount,
            String newBatchStatus,
            String newDownloadBatchStatus
    ) {
        this.videoFilesDeletedCount = videoFilesDeletedCount;
        this.videosUpdatedCount = videosUpdatedCount;
        this.newBatchStatus = newBatchStatus;
        this.newDownloadBatchStatus = newDownloadBatchStatus;
    }

    public int getVideoFilesDeletedCount() {
        return videoFilesDeletedCount;
    }

    public int getVideosUpdatedCount() {
        return videosUpdatedCount;
    }

    public String getNewBatchStatus() {
        return newBatchStatus;
    }

    public String getNewDownloadBatchStatus() {
        return newDownloadBatchStatus;
    }
}