package com.hapro.autobyhapro.entity;

public class VideoFileCheckItem {

    private final Long videoFileId;
    private final Long videoId;
    private final String fileType;
    private final String filePath;
    private final boolean oldFileExists;
    private final boolean actualFileExists;

    public VideoFileCheckItem(
            Long videoFileId,
            Long videoId,
            String fileType,
            String filePath,
            boolean oldFileExists,
            boolean actualFileExists
    ) {
        this.videoFileId = videoFileId;
        this.videoId = videoId;
        this.fileType = fileType;
        this.filePath = filePath;
        this.oldFileExists = oldFileExists;
        this.actualFileExists = actualFileExists;
    }

    public Long getVideoFileId() {
        return videoFileId;
    }

    public Long getVideoId() {
        return videoId;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isOldFileExists() {
        return oldFileExists;
    }

    public boolean isActualFileExists() {
        return actualFileExists;
    }
}