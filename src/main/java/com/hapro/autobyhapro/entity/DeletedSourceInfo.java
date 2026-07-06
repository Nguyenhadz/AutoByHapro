package com.hapro.autobyhapro.entity;

public class DeletedSourceInfo {

    private final Long id;
    private final Long oldSourceId;
    private final String oldSourceCode;
    private final Long fanpageId;
    private final String pageCode;
    private final String pageName;
    private final String sourceName;
    private final String channelName;
    private final String sourceUrl;
    private final String sourceType;
    private final String deletedTime;

    public DeletedSourceInfo(
            Long id,
            Long oldSourceId,
            String oldSourceCode,
            Long fanpageId,
            String pageCode,
            String pageName,
            String sourceName,
            String channelName,
            String sourceUrl,
            String sourceType,
            String deletedTime
    ) {
        this.id = id;
        this.oldSourceId = oldSourceId;
        this.oldSourceCode = oldSourceCode;
        this.fanpageId = fanpageId;
        this.pageCode = pageCode;
        this.pageName = pageName;
        this.sourceName = sourceName;
        this.channelName = channelName;
        this.sourceUrl = sourceUrl;
        this.sourceType = sourceType;
        this.deletedTime = deletedTime;
    }

    public Long getId() {
        return id;
    }

    public Long getOldSourceId() {
        return oldSourceId;
    }

    public String getOldSourceCode() {
        return oldSourceCode;
    }

    public Long getFanpageId() {
        return fanpageId;
    }

    public String getPageCode() {
        return pageCode;
    }

    public String getPageName() {
        return pageName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getDeletedTime() {
        return deletedTime;
    }
}