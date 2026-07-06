package com.hapro.autobyhapro.entity;

public class DownloadTarget {

    private final Long fanpageId;
    private final String fanpageCode;
    private final String fanpageName;
    private final int defaultVideoCount;

    private final Long sourceId;
    private final String sourceCode;
    private final String sourceName;
    private final String sourceType;
    private final String sourceUrl;

    public DownloadTarget(
            Long fanpageId,
            String fanpageCode,
            String fanpageName,
            int defaultVideoCount,
            Long sourceId,
            String sourceCode,
            String sourceName,
            String sourceType,
            String sourceUrl
    ) {
        this.fanpageId = fanpageId;
        this.fanpageCode = fanpageCode;
        this.fanpageName = fanpageName;
        this.defaultVideoCount = defaultVideoCount;
        this.sourceId = sourceId;
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.sourceUrl = sourceUrl;
    }

    public Long getFanpageId() {
        return fanpageId;
    }

    public String getFanpageCode() {
        return fanpageCode;
    }

    public String getFanpageName() {
        return fanpageName;
    }

    public int getDefaultVideoCount() {
        return defaultVideoCount;
    }

    public Long getSourceId() {
        return sourceId;
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
}