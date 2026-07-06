package com.hapro.autobyhapro.entity;

public class Source {

    private Long id;
    private Long fanpageId;
    private String fanpageCode;
    private String fanpageName;

    private String sourceCode;
    private String sourceName;
    private String sourceType;
    private String sourceUrl;
    private String channelName;
    private boolean active;
    private String createdTime;

    public Source() {
    }

    public Source(
            Long id,
            Long fanpageId,
            String fanpageCode,
            String fanpageName,
            String sourceCode,
            String sourceName,
            String sourceType,
            String sourceUrl,
            String channelName,
            boolean active,
            String createdTime
    ) {
        this.id = id;
        this.fanpageId = fanpageId;
        this.fanpageCode = fanpageCode;
        this.fanpageName = fanpageName;
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.sourceUrl = sourceUrl;
        this.channelName = channelName;
        this.active = active;
        this.createdTime = createdTime;
    }

    public Long getId() {
        return id;
    }

    public Long getFanpageId() {
        return fanpageId;
    }

    public void setFanpageId(Long fanpageId) {
        this.fanpageId = fanpageId;
    }

    public String getFanpageCode() {
        return fanpageCode;
    }

    public String getFanpageName() {
        return fanpageName;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public boolean isActive() {
        return active;
    }

    public String getCreatedTime() {
        return createdTime;
    }
}