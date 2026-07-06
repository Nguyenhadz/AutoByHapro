package com.hapro.autobyhapro.entity;

public class ManualUploadMarkResult {

    private final Long videoBatchId;
    private final String batchCode;
    private final int markedVideoCount;
    private final String newBatchStatus;
    private final String message;

    public ManualUploadMarkResult(
            Long videoBatchId,
            String batchCode,
            int markedVideoCount,
            String newBatchStatus,
            String message
    ) {
        this.videoBatchId = videoBatchId;
        this.batchCode = batchCode;
        this.markedVideoCount = markedVideoCount;
        this.newBatchStatus = newBatchStatus;
        this.message = message;
    }

    public Long getVideoBatchId() {
        return videoBatchId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public int getMarkedVideoCount() {
        return markedVideoCount;
    }

    public String getNewBatchStatus() {
        return newBatchStatus;
    }

    public String getMessage() {
        return message;
    }
}