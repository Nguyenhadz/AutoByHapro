package com.hapro.autobyhapro.entity;

public class VideoExportRow {

    private final Long videoId;
    private final String pageCode;
    private final String pageName;
    private final String sourceCode;
    private final String sourceName;
    private final String sourceType;
    private final String platformVideoId;
    private final String title;
    private final String status;
    private final String downloadedTime;
    private final String originalUrl;

    public VideoExportRow(
            Long videoId,
            String pageCode,
            String pageName,
            String sourceCode,
            String sourceName,
            String sourceType,
            String platformVideoId,
            String title,
            String status,
            String downloadedTime,
            String originalUrl
    ) {
        this.videoId = videoId;
        this.pageCode = pageCode;
        this.pageName = pageName;
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.platformVideoId = platformVideoId;
        this.title = title;
        this.status = status;
        this.downloadedTime = downloadedTime;
        this.originalUrl = originalUrl;
    }

    public Long getVideoId() {
        return videoId;
    }

    public String getPageCode() {
        return pageCode;
    }

    public String getPageName() {
        return pageName;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceType() {
        return sourceType;
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

    public String getDownloadedTime() {
        return downloadedTime;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }
}