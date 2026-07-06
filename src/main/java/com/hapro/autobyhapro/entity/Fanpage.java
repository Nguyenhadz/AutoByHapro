package com.hapro.autobyhapro.entity;

public class Fanpage {

    private Long id;
    private String pageCode;
    private String pageName;
    private String pageUrl;
    private String niche;
    private int defaultVideoCount;
    private String startDate;
    private boolean active;
    private String createdTime;

    public Fanpage() {
    }

    public Fanpage(
            Long id,
            String pageCode,
            String pageName,
            String pageUrl,
            String niche,
            int defaultVideoCount,
            String startDate,
            boolean active,
            String createdTime
    ) {
        this.id = id;
        this.pageCode = pageCode;
        this.pageName = pageName;
        this.pageUrl = pageUrl;
        this.niche = niche;
        this.defaultVideoCount = defaultVideoCount;
        this.startDate = startDate;
        this.active = active;
        this.createdTime = createdTime;
    }

    public Long getId() {
        return id;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public int getDefaultVideoCount() {
        return defaultVideoCount;
    }

    public void setDefaultVideoCount(int defaultVideoCount) {
        this.defaultVideoCount = defaultVideoCount;
    }

    public String getStartDate() {
        return startDate;
    }

    public boolean isActive() {
        return active;
    }

    public String getCreatedTime() {
        return createdTime;
    }
}