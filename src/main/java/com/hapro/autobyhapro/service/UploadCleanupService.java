package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.CleanupDatabaseResult;
import com.hapro.autobyhapro.entity.UploadCleanupResult;
import com.hapro.autobyhapro.entity.UploadedMarkedBatch;
import com.hapro.autobyhapro.repository.ManualUploadRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class UploadCleanupService {

    private final ManualUploadRepository manualUploadRepository = new ManualUploadRepository();

    public UploadCleanupResult cleanupUploadedBatch(UploadedMarkedBatch batch) {
        if (batch == null) {
            throw new RuntimeException("Batch cần dọn đang bị trống.");
        }

        List<String> filePaths = manualUploadRepository.findUploadedMarkedFilePaths(
                batch.getVideoBatchId()
        );

        FileDeleteResult deleteResult = deleteRecordedFilesSafely(filePaths);

        if (!deleteResult.success()) {
            return new UploadCleanupResult(
                    batch.getVideoBatchId(),
                    batch.getBatchCode(),
                    deleteResult.rawDeletedFileCount(),
                    deleteResult.editedDeletedFileCount(),
                    false,
                    false,
                    0,
                    0,
                    "FAILED",
                    "Xóa file chưa thành công nên chưa dọn DB. " + deleteResult.message()
            );
        }

        CleanupDatabaseResult databaseResult = manualUploadRepository.cleanupUploadedBatchInDatabase(
                batch.getVideoBatchId()
        );

        return new UploadCleanupResult(
                batch.getVideoBatchId(),
                batch.getBatchCode(),
                deleteResult.rawDeletedFileCount(),
                deleteResult.editedDeletedFileCount(),
                isFolderGoneOrEmpty(batch.getRawFolderPath()),
                isFolderGoneOrEmpty(batch.getEditedFolderPath()),
                databaseResult.getVideoFilesDeletedCount(),
                databaseResult.getVideosUpdatedCount(),
                "CLEANED",
                "Đã xóa file sau upload và dọn DB. Batch status mới: "
                        + databaseResult.getNewBatchStatus()
        );
    }

    private FileDeleteResult deleteRecordedFilesSafely(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return new FileDeleteResult(
                    true,
                    0,
                    0,
                    "Không có file_path trong DB, chỉ dọn DB."
            );
        }

        int rawDeletedFileCount = 0;
        int editedDeletedFileCount = 0;
        StringBuilder messageBuilder = new StringBuilder();

        for (String filePathText : filePaths) {
            if (filePathText == null || filePathText.isBlank()) {
                continue;
            }

            Path filePath = Path.of(filePathText).toAbsolutePath().normalize();

            if (!isInsideSafeVideoFolder(filePath)) {
                return new FileDeleteResult(
                        false,
                        rawDeletedFileCount,
                        editedDeletedFileCount,
                        "File không nằm trong vùng an toàn: " + filePath
                );
            }

            try {
                if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                    Files.deleteIfExists(filePath);

                    if (isInsideFolder(filePath, AppPaths.RAW_DIR)) {
                        rawDeletedFileCount++;
                    } else if (isInsideFolder(filePath, AppPaths.EDITED_DIR)) {
                        editedDeletedFileCount++;
                    }
                }

                deleteParentFoldersIfEmpty(filePath.getParent());

            } catch (IOException exception) {
                return new FileDeleteResult(
                        false,
                        rawDeletedFileCount,
                        editedDeletedFileCount,
                        "Không thể xóa file: "
                                + filePath
                                + " | "
                                + exception.getMessage()
                );
            }
        }

        messageBuilder.append("Đã xóa file đã ghi trong DB. Raw: ")
                .append(rawDeletedFileCount)
                .append(", Edited: ")
                .append(editedDeletedFileCount)
                .append(". Không xóa cả folder page nếu vẫn còn file khác.");

        return new FileDeleteResult(
                true,
                rawDeletedFileCount,
                editedDeletedFileCount,
                messageBuilder.toString()
        );
    }

    private boolean isInsideSafeVideoFolder(Path filePath) {
        return isInsideFolder(filePath, AppPaths.RAW_DIR)
                || isInsideFolder(filePath, AppPaths.EDITED_DIR)
                || isInsideFolder(filePath, AppPaths.CAPCUT_EXPORT_DIR)
                || isInsideFolder(filePath, AppPaths.EDITED_UNKNOWN_DIR);
    }

    private boolean isInsideFolder(Path path, Path folder) {
        if (path == null || folder == null) {
            return false;
        }

        Path cleanPath = path.toAbsolutePath().normalize();
        Path cleanFolder = folder.toAbsolutePath().normalize();

        return cleanPath.startsWith(cleanFolder)
                && !cleanPath.equals(cleanFolder);
    }

    private void deleteParentFoldersIfEmpty(Path startFolder) {
        if (startFolder == null) {
            return;
        }

        Path folder = startFolder.toAbsolutePath().normalize();

        while (folder != null && isDeletableVideoSubFolder(folder)) {
            try {
                if (!Files.exists(folder) || !Files.isDirectory(folder)) {
                    folder = folder.getParent();
                    continue;
                }

                try (var stream = Files.list(folder)) {
                    if (stream.findAny().isPresent()) {
                        return;
                    }
                }

                Files.deleteIfExists(folder);
                folder = folder.getParent();

            } catch (IOException exception) {
                return;
            }
        }
    }

    private boolean isDeletableVideoSubFolder(Path folder) {
        if (folder == null) {
            return false;
        }

        Path cleanFolder = folder.toAbsolutePath().normalize();

        return isChildFolder(cleanFolder, AppPaths.RAW_DIR)
                || isChildFolder(cleanFolder, AppPaths.EDITED_DIR)
                || isChildFolder(cleanFolder, AppPaths.CAPCUT_EXPORT_DIR)
                || isChildFolder(cleanFolder, AppPaths.EDITED_UNKNOWN_DIR);
    }

    private boolean isChildFolder(Path folder, Path rootFolder) {
        Path cleanRoot = rootFolder.toAbsolutePath().normalize();

        return folder.startsWith(cleanRoot)
                && !folder.equals(cleanRoot);
    }

    private boolean isFolderGoneOrEmpty(String folderPathText) {
        if (folderPathText == null || folderPathText.isBlank()) {
            return true;
        }

        try {
            Path folder = Path.of(folderPathText).toAbsolutePath().normalize();

            if (!Files.exists(folder)) {
                return true;
            }

            if (!Files.isDirectory(folder)) {
                return true;
            }

            try (var stream = Files.list(folder)) {
                return stream.findAny().isEmpty();
            }

        } catch (Exception exception) {
            return false;
        }
    }

    private record FileDeleteResult(
            boolean success,
            int rawDeletedFileCount,
            int editedDeletedFileCount,
            String message
    ) {
    }
}
