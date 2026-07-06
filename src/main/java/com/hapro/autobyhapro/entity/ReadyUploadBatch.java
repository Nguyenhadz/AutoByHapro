package com.hapro.autobyhapro.entity;

public class ReadyUploadBatch {

    private final Long videoBatchId;
    private final String batchCode;
    private final String pageCode;
    private final String pageName;
    private final String editedFolderPath;
    private final String batchStatus;
    private final int readyVideoCount;
    private final int totalVideoCount;

    public ReadyUploadBatch(
            Long videoBatchId,
            String batchCode,
            String pageCode,
            String pageName,
            String editedFolderPath,
            String batchStatus,
            int readyVideoCount,
            int totalVideoCount
    ) {
        this.videoBatchId = videoBatchId;
        this.batchCode = batchCode;
        this.pageCode = pageCode;
        this.pageName = pageName;
        this.editedFolderPath = editedFolderPath;
        this.batchStatus = batchStatus;
        this.readyVideoCount = readyVideoCount;
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

    public String getEditedFolderPath() {
        return editedFolderPath;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public int getReadyVideoCount() {
        return readyVideoCount;
    }

    public int getTotalVideoCount() {
        return totalVideoCount;
    }
}
