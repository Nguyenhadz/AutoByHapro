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

    private static final int YT_DLP_TIMEOUT_SECONDS = 60;

    private final YtDlpService ytDlpService = new YtDlpService();
    private final VideoRepository videoRepository = new VideoRepository();
    private final TikTokChannelResolverService tiktokChannelResolverService =
            new TikTokChannelResolverService();

    public SourceScanResult findNewVideos(Source source, int scanLimit) {
        if (source == null) {
            throw new RuntimeException("Source đang bị trống.");
        }

        int safeScanLimit = Math.max(1, scanLimit);

        String sourceUrlForScan = tiktokChannelResolverService.resolveSourceUrlForUse(source);

        if (sourceUrlForScan == null || sourceUrlForScan.isBlank()) {
            sourceUrlForScan = source.getSourceUrl();
        }

        System.out.println("Đang đọc tối đa " + safeScanLimit + " video từ source...");
        System.out.println("URL dùng để quét: " + sourceUrlForScan);

        List<VideoCandidate> scannedVideos = ytDlpService.listVideos(
                sourceUrlForScan,
                safeScanLimit,
                YT_DLP_TIMEOUT_SECONDS
        );

        ScanFilterResult filterResult = filterNewVideos(
                source.getId(),
                scannedVideos,
                safeScanLimit
        );

        return new SourceScanResult(
                source,
                safeScanLimit,
                scannedVideos.size(),
                filterResult.skippedExistingCount(),
                filterResult.newVideos()
        );
    }

    private ScanFilterResult filterNewVideos(
            Long sourceId,
            List<VideoCandidate> scannedVideos,
            int maxNewVideoCount
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

            if (newVideos.size() < maxNewVideoCount) {
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
