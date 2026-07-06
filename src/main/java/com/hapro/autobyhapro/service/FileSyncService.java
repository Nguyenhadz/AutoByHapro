package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.entity.FileSyncResult;
import com.hapro.autobyhapro.entity.VideoFileCheckItem;
import com.hapro.autobyhapro.repository.FileSyncRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileSyncService {

    private final FileSyncRepository fileSyncRepository = new FileSyncRepository();

    public FileSyncResult syncDatabaseWithRealFiles() {
        List<VideoFileCheckItem> videoFiles = fileSyncRepository.findAllVideoFiles();

        int actualExistingFileCount = 0;
        int actualMissingFileCount = 0;
        int updatedFileRecordCount = 0;

        for (VideoFileCheckItem item : videoFiles) {
            boolean actualExists = isRealFileExists(item.getFilePath());

            if (actualExists) {
                actualExistingFileCount++;
            } else {
                actualMissingFileCount++;
            }

            if (item.isOldFileExists() != actualExists) {
                fileSyncRepository.updateVideoFileExists(
                        item.getVideoFileId(),
                        actualExists
                );

                updatedFileRecordCount++;
            }
        }

        int videosChangedToDownloadedCount =
                fileSyncRepository.updateReadyUploadMissingEditedBackToDownloaded();

        int videosChangedToFileMissingCount =
                fileSyncRepository.updateActiveVideosMissingAllFiles();

        int videoBatchStatusUpdatedCount =
                fileSyncRepository.refreshVideoBatchStatuses();

        return new FileSyncResult(
                videoFiles.size(),
                actualExistingFileCount,
                actualMissingFileCount,
                updatedFileRecordCount,
                videosChangedToDownloadedCount,
                videosChangedToFileMissingCount,
                videoBatchStatusUpdatedCount
        );
    }

    private boolean isRealFileExists(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }

        try {
            Path path = Path.of(filePath);

            return Files.exists(path) && Files.isRegularFile(path);

        } catch (Exception exception) {
            return false;
        }
    }
}