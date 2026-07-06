package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.CleanupDatabaseResult;
import com.hapro.autobyhapro.entity.UploadCleanupResult;
import com.hapro.autobyhapro.entity.UploadedMarkedBatch;
import com.hapro.autobyhapro.repository.ManualUploadRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class UploadCleanupService {

    private final ManualUploadRepository manualUploadRepository = new ManualUploadRepository();

    public UploadCleanupResult cleanupUploadedBatch(UploadedMarkedBatch batch) {
        if (batch == null) {
            throw new RuntimeException("Batch cần dọn đang bị trống.");
        }

        FolderDeleteResult rawDeleteResult = deleteBatchFolderSafely(
                batch.getRawFolderPath(),
                AppPaths.RAW_DIR
        );

        FolderDeleteResult editedDeleteResult = deleteBatchFolderSafely(
                batch.getEditedFolderPath(),
                AppPaths.EDITED_DIR
        );

        if (!rawDeleteResult.success() || !editedDeleteResult.success()) {
            return new UploadCleanupResult(
                    batch.getVideoBatchId(),
                    batch.getBatchCode(),
                    rawDeleteResult.deletedFileCount(),
                    editedDeleteResult.deletedFileCount(),
                    rawDeleteResult.folderDeleted(),
                    editedDeleteResult.folderDeleted(),
                    0,
                    0,
                    "FAILED",
                    "Xóa file/folder chưa thành công nên chưa dọn DB. Raw: "
                            + rawDeleteResult.message()
                            + " | Edited: "
                            + editedDeleteResult.message()
            );
        }

        CleanupDatabaseResult databaseResult =
                manualUploadRepository.cleanupUploadedBatchInDatabase(batch.getVideoBatchId());

        return new UploadCleanupResult(
                batch.getVideoBatchId(),
                batch.getBatchCode(),
                rawDeleteResult.deletedFileCount(),
                editedDeleteResult.deletedFileCount(),
                rawDeleteResult.folderDeleted(),
                editedDeleteResult.folderDeleted(),
                databaseResult.getVideoFilesDeletedCount(),
                databaseResult.getVideosUpdatedCount(),
                "CLEANED",
                "Đã xóa file sau upload và dọn DB. Batch status mới: "
                        + databaseResult.getNewBatchStatus()
        );
    }

    private FolderDeleteResult deleteBatchFolderSafely(
            String folderPath,
            Path safeRootFolder
    ) {
        if (folderPath == null || folderPath.isBlank()) {
            return new FolderDeleteResult(
                    true,
                    0,
                    true,
                    "Không có đường dẫn folder, bỏ qua."
            );
        }

        Path folder = Path.of(folderPath).toAbsolutePath().normalize();
        Path safeRoot = safeRootFolder.toAbsolutePath().normalize();

        if (!folder.startsWith(safeRoot)) {
            return new FolderDeleteResult(
                    false,
                    0,
                    false,
                    "Folder không nằm trong vùng an toàn: " + folder
            );
        }

        if (folder.equals(safeRoot)) {
            return new FolderDeleteResult(
                    false,
                    0,
                    false,
                    "Không được xóa folder gốc: " + folder
            );
        }

        if (!Files.exists(folder)) {
            return new FolderDeleteResult(
                    true,
                    0,
                    true,
                    "Folder không tồn tại, coi như đã dọn."
            );
        }

        if (!Files.isDirectory(folder)) {
            return new FolderDeleteResult(
                    false,
                    0,
                    false,
                    "Đường dẫn không phải folder: " + folder
            );
        }

        try {
            List<Path> paths = Files.walk(folder)
                    .sorted(Comparator.reverseOrder())
                    .toList();

            int deletedFileCount = 0;

            for (Path path : paths) {
                if (Files.isRegularFile(path)) {
                    deletedFileCount++;
                }

                Files.deleteIfExists(path);
            }

            deleteParentIfEmpty(folder.getParent(), safeRoot);

            return new FolderDeleteResult(
                    true,
                    deletedFileCount,
                    !Files.exists(folder),
                    "Đã xóa folder batch."
            );

        } catch (IOException exception) {
            return new FolderDeleteResult(
                    false,
                    0,
                    false,
                    exception.getMessage()
            );
        }
    }

    private void deleteParentIfEmpty(Path parentFolder, Path safeRootFolder) {
        if (parentFolder == null) {
            return;
        }

        Path parent = parentFolder.toAbsolutePath().normalize();
        Path safeRoot = safeRootFolder.toAbsolutePath().normalize();

        if (parent.equals(safeRoot)) {
            return;
        }

        if (!parent.startsWith(safeRoot)) {
            return;
        }

        try {
            if (Files.exists(parent) && Files.isDirectory(parent)) {
                try (var stream = Files.list(parent)) {
                    if (stream.findAny().isEmpty()) {
                        Files.deleteIfExists(parent);
                    }
                }
            }
        } catch (IOException ignored) {
            // Không cần báo lỗi nếu parent folder chưa xóa được.
        }
    }

    private record FolderDeleteResult(
            boolean success,
            int deletedFileCount,
            boolean folderDeleted,
            String message
    ) {
    }
}