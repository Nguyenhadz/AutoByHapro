package com.hapro.autobyhapro.entity;

public class SourceCheckResult {

    private final Long sourceId;
    private final String pageCode;
    private final String pageName;
    private final String sourceCode;
    private final String sourceName;
    private final String sourceType;
    private final String sourceUrl;
    private final String channelName;
    private final boolean active;

    private final String status;
    private final int totalFoundCount;
    private final int alreadyDownloadedCount;
    private final int notYetDownloadedCount;
    private final String message;

    public SourceCheckResult(
            Long sourceId,
            String pageCode,
            String pageName,
            String sourceCode,
            String sourceName,
            String sourceType,
            String sourceUrl,
            String channelName,
            boolean active,
            String status,
            int totalFoundCount,
            int alreadyDownloadedCount,
            int notYetDownloadedCount,
            String message
    ) {
        this.sourceId = sourceId;
        this.pageCode = pageCode;
        this.pageName = pageName;
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.sourceUrl = sourceUrl;
        this.channelName = channelName;
        this.active = active;
        this.status = status;
        this.totalFoundCount = totalFoundCount;
        this.alreadyDownloadedCount = alreadyDownloadedCount;
        this.notYetDownloadedCount = notYetDownloadedCount;
        this.message = message;
    }

    public Long getSourceId() {
        return sourceId;
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

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getChannelName() {
        return channelName;
    }

    public boolean isActive() {
        return active;
    }

    public String getStatus() {
        return status;
    }

    public int getTotalFoundCount() {
        return totalFoundCount;
    }

    public int getAlreadyDownloadedCount() {
        return alreadyDownloadedCount;
    }

    public int getNotYetDownloadedCount() {
        return notYetDownloadedCount;
    }

    public String getMessage() {
        return message;
    }

    public SourceCheckResult withCheckResult(
            String status,
            int totalFoundCount,
            int alreadyDownloadedCount,
            int notYetDownloadedCount,
            String message
    ) {
        return new SourceCheckResult(
                sourceId,
                pageCode,
                pageName,
                sourceCode,
                sourceName,
                sourceType,
                sourceUrl,
                channelName,
                active,
                status,
                totalFoundCount,
                alreadyDownloadedCount,
                notYetDownloadedCount,
                message
        );
    }
}