package com.hapro.autobyhapro.entity;

public class PageSimpleStats {

    private final Long fanpageId;
    private final String pageCode;
    private final String pageName;
    private final String sourceCode;
    private final String sourceName;
    private final String sourceType;
    private final String startDate;
    private final int totalDownloadedCount;
    private final int readyUploadCount;
    private final int uploadedMarkedCount;
    private final int uploadedDeletedCount;
    private final int uploadedTotalCount;
    private final int workingDays;

    public PageSimpleStats(
            Long fanpageId,
            String pageCode,
            String pageName,
            String sourceCode,
            String sourceName,
            String sourceType,
            String startDate,
            int totalDownloadedCount,
            int readyUploadCount,
            int uploadedMarkedCount,
            int uploadedDeletedCount,
            int uploadedTotalCount,
            int workingDays
    ) {
        this.fanpageId = fanpageId;
        this.pageCode = pageCode;
        this.pageName = pageName;
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.startDate = startDate;
        this.totalDownloadedCount = totalDownloadedCount;
        this.readyUploadCount = readyUploadCount;
        this.uploadedMarkedCount = uploadedMarkedCount;
        this.uploadedDeletedCount = uploadedDeletedCount;
        this.uploadedTotalCount = uploadedTotalCount;
        this.workingDays = workingDays;
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

    public String getSourceCode() {
        return sourceCode;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getStartDate() {
        return startDate;
    }

    public int getTotalDownloadedCount() {
        return totalDownloadedCount;
    }

    public int getReadyUploadCount() {
        return readyUploadCount;
    }

    public int getUploadedMarkedCount() {
        return uploadedMarkedCount;
    }

    public int getUploadedDeletedCount() {
        return uploadedDeletedCount;
    }

    public int getUploadedTotalCount() {
        return uploadedTotalCount;
    }

    public int getWorkingDays() {
        return workingDays;
    }
}