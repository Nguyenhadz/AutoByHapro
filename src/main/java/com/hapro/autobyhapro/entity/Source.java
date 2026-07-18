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

    private String tiktokChannelId;
    private String resolvedSourceUrl;
    private String resolvedTime;
    private String resolvedStatus;
    private String resolveMessage;

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
        this(
                id,
                fanpageId,
                fanpageCode,
                fanpageName,
                sourceCode,
                sourceName,
                sourceType,
                sourceUrl,
                channelName,
                active,
                createdTime,
                "",
                "",
                "",
                "",
                ""
        );
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
            String createdTime,
            String tiktokChannelId,
            String resolvedSourceUrl,
            String resolvedTime,
            String resolvedStatus,
            String resolveMessage
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
        this.tiktokChannelId = tiktokChannelId;
        this.resolvedSourceUrl = resolvedSourceUrl;
        this.resolvedTime = resolvedTime;
        this.resolvedStatus = resolvedStatus;
        this.resolveMessage = resolveMessage;
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

    public String getTiktokChannelId() {
        return tiktokChannelId;
    }

    public void setTiktokChannelId(String tiktokChannelId) {
        this.tiktokChannelId = tiktokChannelId;
    }

    public String getResolvedSourceUrl() {
        return resolvedSourceUrl;
    }

    public void setResolvedSourceUrl(String resolvedSourceUrl) {
        this.resolvedSourceUrl = resolvedSourceUrl;
    }

    public String getResolvedTime() {
        return resolvedTime;
    }

    public void setResolvedTime(String resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    public String getResolvedStatus() {
        return resolvedStatus;
    }

    public void setResolvedStatus(String resolvedStatus) {
        this.resolvedStatus = resolvedStatus;
    }

    public String getResolveMessage() {
        return resolveMessage;
    }

    public void setResolveMessage(String resolveMessage) {
        this.resolveMessage = resolveMessage;
    }

    public boolean isTikTokSource() {
        if (sourceType != null && sourceType.equalsIgnoreCase("TIKTOK")) {
            return true;
        }

        if (sourceUrl == null) {
            return false;
        }

        String lowerUrl = sourceUrl.toLowerCase();

        return lowerUrl.contains("tiktok.com")
                || lowerUrl.contains("vm.tiktok.com")
                || lowerUrl.contains("vt.tiktok.com")
                || lowerUrl.startsWith("tiktokuser:");
    }

    public String getEffectiveSourceUrl() {
        if (isTikTokSource()
                && resolvedSourceUrl != null
                && !resolvedSourceUrl.isBlank()) {
            return resolvedSourceUrl.trim();
        }

        if (sourceUrl == null) {
            return "";
        }

        return sourceUrl.trim();
    }
}
