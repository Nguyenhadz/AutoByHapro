package com.hapro.autobyhapro.entity;

import java.util.List;

public class SourceScanResult {

    private final Source source;
    private final int requestedCount;
    private final int scannedCount;
    private final int skippedExistingCount;
    private final List<VideoCandidate> newVideos;

    public SourceScanResult(
            Source source,
            int requestedCount,
            int scannedCount,
            int skippedExistingCount,
            List<VideoCandidate> newVideos
    ) {
        this.source = source;
        this.requestedCount = requestedCount;
        this.scannedCount = scannedCount;
        this.skippedExistingCount = skippedExistingCount;
        this.newVideos = newVideos;
    }

    public Source getSource() {
        return source;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public int getScannedCount() {
        return scannedCount;
    }

    public int getSkippedExistingCount() {
        return skippedExistingCount;
    }

    public List<VideoCandidate> getNewVideos() {
        return newVideos;
    }

    public boolean hasEnoughVideos() {
        return newVideos.size() >= requestedCount;
    }
}
