package com.hapro.autobyhapro.entity;

public class FileSyncResult {

    private final int totalFileRecordCount;
    private final int actualExistingFileCount;
    private final int actualMissingFileCount;
    private final int updatedFileRecordCount;
    private final int videosChangedToDownloadedCount;
    private final int videosChangedToFileMissingCount;
    private final int videoBatchStatusUpdatedCount;

    public FileSyncResult(
            int totalFileRecordCount,
            int actualExistingFileCount,
            int actualMissingFileCount,
            int updatedFileRecordCount,
            int videosChangedToDownloadedCount,
            int videosChangedToFileMissingCount,
            int videoBatchStatusUpdatedCount
    ) {
        this.totalFileRecordCount = totalFileRecordCount;
        this.actualExistingFileCount = actualExistingFileCount;
        this.actualMissingFileCount = actualMissingFileCount;
        this.updatedFileRecordCount = updatedFileRecordCount;
        this.videosChangedToDownloadedCount = videosChangedToDownloadedCount;
        this.videosChangedToFileMissingCount = videosChangedToFileMissingCount;
        this.videoBatchStatusUpdatedCount = videoBatchStatusUpdatedCount;
    }

    public int getTotalFileRecordCount() {
        return totalFileRecordCount;
    }

    public int getActualExistingFileCount() {
        return actualExistingFileCount;
    }

    public int getActualMissingFileCount() {
        return actualMissingFileCount;
    }

    public int getUpdatedFileRecordCount() {
        return updatedFileRecordCount;
    }

    public int getVideosChangedToDownloadedCount() {
        return videosChangedToDownloadedCount;
    }

    public int getVideosChangedToFileMissingCount() {
        return videosChangedToFileMissingCount;
    }

    public int getVideoBatchStatusUpdatedCount() {
        return videoBatchStatusUpdatedCount;
    }
}
