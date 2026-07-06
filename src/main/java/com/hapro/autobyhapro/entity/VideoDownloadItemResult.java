package com.hapro.autobyhapro.entity;

public class VideoDownloadItemResult {

    private final VideoCandidate video;
    private final Long videoBatchId;
    private final String batchCode;
    private final boolean success;
    private final String filePath;
    private final String message;

    public VideoDownloadItemResult(
            VideoCandidate video,
            Long videoBatchId,
            String batchCode,
            boolean success,
            String filePath,
            String message
    ) {
        this.video = video;
        this.videoBatchId = videoBatchId;
        this.batchCode = batchCode;
        this.success = success;
        this.filePath = filePath;
        this.message = message;
    }

    public VideoCandidate getVideo() {
        return video;
    }

    public Long getVideoBatchId() {
        return videoBatchId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getMessage() {
        return message;
    }
}