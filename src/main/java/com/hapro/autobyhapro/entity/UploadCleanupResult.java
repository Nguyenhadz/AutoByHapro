package com.hapro.autobyhapro.entity;

public class UploadCleanupResult {

    private final Long videoBatchId;
    private final String batchCode;
    private final int rawDeletedFileCount;
    private final int editedDeletedFileCount;
    private final boolean rawFolderDeleted;
    private final boolean editedFolderDeleted;
    private final int databaseVideoFilesDeletedCount;
    private final int databaseVideosUpdatedCount;
    private final String status;
    private final String message;

    public UploadCleanupResult(
            Long videoBatchId,
            String batchCode,
            int rawDeletedFileCount,
            int editedDeletedFileCount,
            boolean rawFolderDeleted,
            boolean editedFolderDeleted,
            int databaseVideoFilesDeletedCount,
            int databaseVideosUpdatedCount,
            String status,
            String message
    ) {
        this.videoBatchId = videoBatchId;
        this.batchCode = batchCode;
        this.rawDeletedFileCount = rawDeletedFileCount;
        this.editedDeletedFileCount = editedDeletedFileCount;
        this.rawFolderDeleted = rawFolderDeleted;
        this.editedFolderDeleted = editedFolderDeleted;
        this.databaseVideoFilesDeletedCount = databaseVideoFilesDeletedCount;
        this.databaseVideosUpdatedCount = databaseVideosUpdatedCount;
        this.status = status;
        this.message = message;
    }

    public Long getVideoBatchId() {
        return videoBatchId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public int getRawDeletedFileCount() {
        return rawDeletedFileCount;
    }

    public int getEditedDeletedFileCount() {
        return editedDeletedFileCount;
    }

    public boolean isRawFolderDeleted() {
        return rawFolderDeleted;
    }

    public boolean isEditedFolderDeleted() {
        return editedFolderDeleted;
    }

    public int getDatabaseVideoFilesDeletedCount() {
        return databaseVideoFilesDeletedCount;
    }

    public int getDatabaseVideosUpdatedCount() {
        return databaseVideosUpdatedCount;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
