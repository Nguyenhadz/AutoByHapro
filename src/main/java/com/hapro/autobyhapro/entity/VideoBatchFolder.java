package com.hapro.autobyhapro.entity;

import java.nio.file.Path;

public class VideoBatchFolder {

    private final Long videoBatchId;
    private final String batchCode;
    private final int videoCount;
    private final Path rawFolderPath;
    private final Path editedFolderPath;

    public VideoBatchFolder(
            Long videoBatchId,
            String batchCode,
            int videoCount,
            Path rawFolderPath,
            Path editedFolderPath
    ) {
        this.videoBatchId = videoBatchId;
        this.batchCode = batchCode;
        this.videoCount = videoCount;
        this.rawFolderPath = rawFolderPath;
        this.editedFolderPath = editedFolderPath;
    }

    public Long getVideoBatchId() {
        return videoBatchId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public Path getRawFolderPath() {
        return rawFolderPath;
    }

    public Path getEditedFolderPath() {
        return editedFolderPath;
    }
}
