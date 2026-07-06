package com.hapro.autobyhapro.entity;

public class UploadedMarkedBatch {

    private final Long videoBatchId;
    private final String batchCode;
    private final String pageCode;
    private final String pageName;
    private final String rawFolderPath;
    private final String editedFolderPath;
    private final String batchStatus;
    private final int uploadedMarkedVideoCount;
    private final int totalVideoCount;

    public UploadedMarkedBatch(
            Long videoBatchId,
            String batchCode,
            String pageCode,
            String pageName,
            String rawFolderPath,
            String editedFolderPath,
            String batchStatus,
            int uploadedMarkedVideoCount,
            int totalVideoCount
    ) {
        this.videoBatchId = videoBatchId;
        this.batchCode = batchCode;
        this.pageCode = pageCode;
        this.pageName = pageName;
        this.rawFolderPath = rawFolderPath;
        this.editedFolderPath = editedFolderPath;
        this.batchStatus = batchStatus;
        this.uploadedMarkedVideoCount = uploadedMarkedVideoCount;
        this.totalVideoCount = totalVideoCount;
    }

    public Long getVideoBatchId() {
        return videoBatchId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public String getPageCode() {
        return pageCode;
    }

    public String getPageName() {
        return pageName;
    }

    public String getRawFolderPath() {
        return rawFolderPath;
    }

    public String getEditedFolderPath() {
        return editedFolderPath;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public int getUploadedMarkedVideoCount() {
        return uploadedMarkedVideoCount;
    }

    public int getTotalVideoCount() {
        return totalVideoCount;
    }
}
