package com.hapro.autobyhapro.entity;

public class EditedVideoSortItemResult {

    private final String sourceFilePath;
    private final String destinationFilePath;
    private final String videoId;
    private final String batchCode;
    private final boolean success;
    private final String status;
    private final String message;

    public EditedVideoSortItemResult(
            String sourceFilePath,
            String destinationFilePath,
            String videoId,
            String batchCode,
            boolean success,
            String status,
            String message
    ) {
        this.sourceFilePath = sourceFilePath;
        this.destinationFilePath = destinationFilePath;
        this.videoId = videoId;
        this.batchCode = batchCode;
        this.success = success;
        this.status = status;
        this.message = message;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public String getDestinationFilePath() {
        return destinationFilePath;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
