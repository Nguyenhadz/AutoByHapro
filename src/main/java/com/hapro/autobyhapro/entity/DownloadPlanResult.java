package com.hapro.autobyhapro.entity;

import java.util.List;

public class DownloadPlanResult {

    private final Long downloadBatchId;
    private final DownloadTarget downloadTarget;
    private final int requestedCount;
    private final int threadCount;
    private final List<VideoBatchFolder> videoBatchFolders;

    public DownloadPlanResult(
            Long downloadBatchId,
            DownloadTarget downloadTarget,
            int requestedCount,
            int threadCount,
            List<VideoBatchFolder> videoBatchFolders
    ) {
        this.downloadBatchId = downloadBatchId;
        this.downloadTarget = downloadTarget;
        this.requestedCount = requestedCount;
        this.threadCount = threadCount;
        this.videoBatchFolders = videoBatchFolders;
    }

    public Long getDownloadBatchId() {
        return downloadBatchId;
    }

    public DownloadTarget getDownloadTarget() {
        return downloadTarget;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public List<VideoBatchFolder> getVideoBatchFolders() {
        return videoBatchFolders;
    }
}