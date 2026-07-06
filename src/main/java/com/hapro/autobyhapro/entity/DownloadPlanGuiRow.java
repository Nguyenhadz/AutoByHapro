package com.hapro.autobyhapro.entity;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class DownloadPlanGuiRow {

    private final Long fanpageId;
    private final String pageCode;
    private final String pageName;
    private final String sourceCode;
    private final String sourceName;
    private final String sourceType;
    private final String sourceUrl;
    private final Long sourceId;
    private final int defaultVideoCount;

    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final IntegerProperty downloadCount = new SimpleIntegerProperty(6);

    public DownloadPlanGuiRow(
            Long fanpageId,
            String pageCode,
            String pageName,
            Long sourceId,
            String sourceCode,
            String sourceName,
            String sourceType,
            String sourceUrl,
            int defaultVideoCount
    ) {
        this.fanpageId = fanpageId;
        this.pageCode = pageCode;
        this.pageName = pageName;
        this.sourceId = sourceId;
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.sourceUrl = sourceUrl;
        this.defaultVideoCount = defaultVideoCount;
        this.downloadCount.set(defaultVideoCount > 0 ? defaultVideoCount : 6);
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

    public int getDefaultVideoCount() {
        return defaultVideoCount;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public int getDownloadCount() {
        return downloadCount.get();
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount.set(downloadCount);
    }

    public IntegerProperty downloadCountProperty() {
        return downloadCount;
    }
}