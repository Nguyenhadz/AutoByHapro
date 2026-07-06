package com.hapro.autobyhapro.entity;

public class EditedVideoTarget {

    private final Long videoId;
    private final Long batchId;
    private final String platformVideoId;
    private final String title;
    private final String status;
    private final String batchCode;
    private final String editedFolderPath;
    private final String rawFilePath;

    public EditedVideoTarget(
            Long videoId,
            Long batchId,
            String platformVideoId,
            String title,
            String status,
            String batchCode,
            String editedFolderPath,
            String rawFilePath
    ) {
        this.videoId = videoId;
        this.batchId = batchId;
        this.platformVideoId = platformVideoId;
        this.title = title;
        this.status = status;
        this.batchCode = batchCode;
        this.editedFolderPath = editedFolderPath;
        this.rawFilePath = rawFilePath;
    }

    public Long getVideoId() {
        return videoId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public String getPlatformVideoId() {
        return platformVideoId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public String getEditedFolderPath() {
        return editedFolderPath;
    }

    public String getRawFilePath() {
        return rawFilePath;
    }
}