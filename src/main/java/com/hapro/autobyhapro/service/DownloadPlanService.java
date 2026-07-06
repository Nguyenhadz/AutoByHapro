package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.DownloadPlanResult;
import com.hapro.autobyhapro.entity.DownloadTarget;
import com.hapro.autobyhapro.entity.VideoBatchFolder;
import com.hapro.autobyhapro.repository.DownloadPlanRepository;
import com.hapro.autobyhapro.util.FileNameUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DownloadPlanService {

    private static final int VIDEO_PER_FOLDER_BATCH = 6;

    private final DownloadPlanRepository downloadPlanRepository = new DownloadPlanRepository();

    public List<DownloadTarget> findActiveDownloadTargets() {
        return downloadPlanRepository.findActiveDownloadTargets();
    }

    public DownloadPlanResult createDownloadPlan(
            DownloadTarget target,
            int requestedCount,
            int threadCount
    ) {
        if (requestedCount <= 0) {
            requestedCount = target.getDefaultVideoCount();
        }

        if (requestedCount <= 0) {
            requestedCount = VIDEO_PER_FOLDER_BATCH;
        }

        if (threadCount <= 0) {
            threadCount = 1;
        }

        long downloadBatchId = downloadPlanRepository.createDownloadBatch(
                target.getFanpageId(),
                target.getSourceId(),
                requestedCount,
                threadCount
        );

        String pageFolderName = FileNameUtil.safeFolderName(
                target.getFanpageCode() + "_" + target.getFanpageName(),
                80
        );

        Path pageRawFolder = AppPaths.RAW_DIR.resolve(pageFolderName);
        Path pageEditedFolder = AppPaths.EDITED_DIR.resolve(pageFolderName);

        createDirectory(pageRawFolder);
        createDirectory(pageEditedFolder);

        int totalBatchCount = (int) Math.ceil(requestedCount / (double) VIDEO_PER_FOLDER_BATCH);
        int remainingVideoCount = requestedCount;

        List<VideoBatchFolder> videoBatchFolders = new ArrayList<>();

        for (int batchIndex = 1; batchIndex <= totalBatchCount; batchIndex++) {
            int batchVideoCount = Math.min(VIDEO_PER_FOLDER_BATCH, remainingVideoCount);

            String batchCode = String.format(
                    "%s__D%06d__B%03d",
                    target.getFanpageCode(),
                    downloadBatchId,
                    batchIndex
            );

            Path rawBatchFolder = pageRawFolder.resolve(batchCode);
            Path editedBatchFolder = pageEditedFolder.resolve(batchCode);

            createDirectory(rawBatchFolder);
            createDirectory(editedBatchFolder);

            long videoBatchId = downloadPlanRepository.createVideoBatch(
                    batchCode,
                    target.getFanpageId(),
                    target.getSourceId(),
                    downloadBatchId,
                    batchIndex,
                    batchVideoCount,
                    rawBatchFolder.toAbsolutePath().toString(),
                    editedBatchFolder.toAbsolutePath().toString()
            );

            VideoBatchFolder videoBatchFolder = new VideoBatchFolder(
                    videoBatchId,
                    batchCode,
                    batchVideoCount,
                    rawBatchFolder,
                    editedBatchFolder
            );

            videoBatchFolders.add(videoBatchFolder);

            remainingVideoCount = remainingVideoCount - batchVideoCount;
        }

        return new DownloadPlanResult(
                downloadBatchId,
                target,
                requestedCount,
                threadCount,
                videoBatchFolders
        );
    }

    private void createDirectory(Path folder) {
        try {
            Files.createDirectories(folder);
        } catch (IOException exception) {
            throw new RuntimeException("Không thể tạo folder: " + folder.toAbsolutePath(), exception);
        }
    }
}