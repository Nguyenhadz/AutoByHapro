package com.hapro.autobyhapro.entity;

import java.util.List;

public class AutoDownloadJobResult {

    private final int threadCount;
    private final List<AutoDownloadPageResult> pageResults;

    public AutoDownloadJobResult(int threadCount, List<AutoDownloadPageResult> pageResults) {
        this.threadCount = threadCount;
        this.pageResults = pageResults;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public List<AutoDownloadPageResult> getPageResults() {
        return pageResults;
    }

    public int getTotalPages() {
        return pageResults.size();
    }

    public int getTotalRequestedCount() {
        int total = 0;

        for (AutoDownloadPageResult result : pageResults) {
            total = total + result.getRequestedCount();
        }

        return total;
    }

    public int getTotalFoundNewCount() {
        int total = 0;

        for (AutoDownloadPageResult result : pageResults) {
            total = total + result.getFoundNewCount();
        }

        return total;
    }

    public int getTotalDownloadedCount() {
        int total = 0;

        for (AutoDownloadPageResult result : pageResults) {
            total = total + result.getDownloadedCount();
        }

        return total;
    }

    public int getTotalFailedCount() {
        int total = 0;

        for (AutoDownloadPageResult result : pageResults) {
            total = total + result.getFailedCount();
        }

        return total;
    }

    public int getTotalFolderCount() {
        int total = 0;

        for (AutoDownloadPageResult result : pageResults) {
            total = total + result.getVideoBatchFolders().size();
        }

        return total;
    }
}