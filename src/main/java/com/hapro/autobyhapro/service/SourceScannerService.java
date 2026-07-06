package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.entity.Source;
import com.hapro.autobyhapro.entity.SourceScanResult;
import com.hapro.autobyhapro.entity.VideoCandidate;
import com.hapro.autobyhapro.repository.VideoRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SourceScannerService {

    private static final int START_SCAN_LIMIT = 12;
    private static final int MAX_SCAN_LIMIT = 500;
    private static final int YT_DLP_TIMEOUT_SECONDS = 60;

    private final YtDlpService ytDlpService = new YtDlpService();
    private final VideoRepository videoRepository = new VideoRepository();

    public SourceScanResult findNewVideos(Source source, int requestedCount) {
        if (source == null) {
            throw new RuntimeException("Source đang bị trống.");
        }

        if (requestedCount <= 0) {
            requestedCount = 6;
        }

        int scanLimit = START_SCAN_LIMIT;
        int lastScannedCount = -1;

        List<VideoCandidate> latestScannedVideos = new ArrayList<>();
        List<VideoCandidate> newVideos = new ArrayList<>();
        int skippedExistingCount = 0;

        while (scanLimit <= MAX_SCAN_LIMIT) {
            System.out.println("Đang đọc tối đa " + scanLimit + " video từ source...");

            latestScannedVideos = ytDlpService.listVideos(
                    source.getSourceUrl(),
                    scanLimit,
                    YT_DLP_TIMEOUT_SECONDS
            );

            if (latestScannedVideos.isEmpty()) {
                break;
            }

            ScanFilterResult filterResult = filterNewVideos(
                    source.getId(),
                    latestScannedVideos,
                    requestedCount
            );

            newVideos = filterResult.newVideos();
            skippedExistingCount = filterResult.skippedExistingCount();

            if (newVideos.size() >= requestedCount) {
                break;
            }

            if (latestScannedVideos.size() == lastScannedCount) {
                break;
            }

            lastScannedCount = latestScannedVideos.size();
            scanLimit = scanLimit * 2;
        }

        return new SourceScanResult(
                source,
                requestedCount,
                latestScannedVideos.size(),
                skippedExistingCount,
                newVideos
        );
    }

    private ScanFilterResult filterNewVideos(
            Long sourceId,
            List<VideoCandidate> scannedVideos,
            int requestedCount
    ) {
        Set<String> scannedIds = new LinkedHashSet<>();

        for (VideoCandidate video : scannedVideos) {
            scannedIds.add(video.getVideoId());
        }

        Set<String> existingIds = videoRepository.findExistingVideoIds(
                sourceId,
                scannedIds
        );

        List<VideoCandidate> newVideos = new ArrayList<>();
        int skippedExistingCount = 0;

        for (VideoCandidate video : scannedVideos) {
            if (existingIds.contains(video.getVideoId())) {
                skippedExistingCount++;
                continue;
            }

            if (newVideos.size() < requestedCount) {
                newVideos.add(video);
            }
        }

        return new ScanFilterResult(
                newVideos,
                skippedExistingCount
        );
    }

    private record ScanFilterResult(
            List<VideoCandidate> newVideos,
            int skippedExistingCount
    ) {
    }
}