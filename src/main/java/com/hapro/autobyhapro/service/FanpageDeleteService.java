package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.Fanpage;
import com.hapro.autobyhapro.entity.FanpageDeleteResult;
import com.hapro.autobyhapro.repository.DeleteManagementRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

public class FanpageDeleteService {

    private final DeleteManagementRepository deleteManagementRepository =
            new DeleteManagementRepository();

    public FanpageDeleteResult hardDeleteFanpage(Fanpage fanpage) {
        if (fanpage == null) {
            throw new RuntimeException("Fanpage đang bị trống.");
        }

        Long fanpageId = fanpage.getId();

        Set<Path> foldersToDelete =
                deleteManagementRepository.findFanpageRelatedFolders(fanpageId);

        int databaseDeletedRows =
                deleteManagementRepository.hardDeleteFanpage(fanpageId);

        FolderCleanupResult folderCleanupResult =
                deleteRelatedFolders(foldersToDelete);

        return new FanpageDeleteResult(
                fanpage.getId(),
                fanpage.getPageCode(),
                fanpage.getPageName(),
                databaseDeletedRows,
                folderCleanupResult.deletedFolderCount(),
                folderCleanupResult.deletedFileCount(),
                folderCleanupResult.message()
        );
    }

    private FolderCleanupResult deleteRelatedFolders(Set<Path> folders) {
        if (folders == null || folders.isEmpty()) {
            return new FolderCleanupResult(
                    0,
                    0,
                    "Không có folder raw/edited cần xóa."
            );
        }

        int deletedFolderCount = 0;
        int deletedFileCount = 0;
        int skippedMissingFolderCount = 0;
        int skippedUnsafeFolderCount = 0;
        int failedFolderCount = 0;

        for (Path folder : folders) {
            if (folder == null) {
                continue;
            }

            DeleteOneFolderResult result = deleteOneFolderSafely(folder);

            deletedFolderCount = deletedFolderCount + result.deletedFolderCount();
            deletedFileCount = deletedFileCount + result.deletedFileCount();

            if (result.status().equals("MISSING")) {
                skippedMissingFolderCount++;
            } else if (result.status().equals("UNSAFE")) {
                skippedUnsafeFolderCount++;
            } else if (result.status().equals("FAILED")) {
                failedFolderCount++;
            }
        }

        String message = "Đã xử lý folder liên quan fanpage."
                + "\nFolder thật sự đã xóa: " + deletedFolderCount
                + "\nFile thật sự đã xóa: " + deletedFileCount
                + "\nFolder không tồn tại đã bỏ qua: " + skippedMissingFolderCount
                + "\nFolder không an toàn đã bỏ qua: " + skippedUnsafeFolderCount
                + "\nFolder xóa thất bại: " + failedFolderCount;

        return new FolderCleanupResult(
                deletedFolderCount,
                deletedFileCount,
                message
        );
    }

    private DeleteOneFolderResult deleteOneFolderSafely(Path folder) {
        Path cleanFolder = folder.toAbsolutePath().normalize();

        if (!isSafeDeleteFolder(cleanFolder)) {
            return new DeleteOneFolderResult(
                    0,
                    0,
                    "UNSAFE"
            );
        }

        if (!Files.exists(cleanFolder)) {
            return new DeleteOneFolderResult(
                    0,
                    0,
                    "MISSING"
            );
        }

        if (!Files.isDirectory(cleanFolder)) {
            return new DeleteOneFolderResult(
                    0,
                    0,
                    "UNSAFE"
            );
        }

        try (Stream<Path> stream = Files.walk(cleanFolder)) {
            var paths = stream
                    .sorted(Comparator.reverseOrder())
                    .toList();

            int deletedFileCount = 0;
            int deletedFolderCount = 0;

            for (Path path : paths) {
                if (Files.isRegularFile(path)) {
                    deletedFileCount++;
                }

                if (Files.isDirectory(path)) {
                    deletedFolderCount++;
                }

                Files.deleteIfExists(path);
            }

            deleteParentIfEmpty(cleanFolder.getParent());

            return new DeleteOneFolderResult(
                    deletedFolderCount,
                    deletedFileCount,
                    "DELETED"
            );

        } catch (IOException exception) {
            return new DeleteOneFolderResult(
                    0,
                    0,
                    "FAILED"
            );
        }
    }

    private boolean isSafeDeleteFolder(Path folder) {
        Path rawRoot = AppPaths.RAW_DIR.toAbsolutePath().normalize();
        Path editedRoot = AppPaths.EDITED_DIR.toAbsolutePath().normalize();

        if (folder.equals(rawRoot) || folder.equals(editedRoot)) {
            return false;
        }

        return folder.startsWith(rawRoot) || folder.startsWith(editedRoot);
    }

    private void deleteParentIfEmpty(Path parentFolder) {
        if (parentFolder == null) {
            return;
        }

        Path parent = parentFolder.toAbsolutePath().normalize();
        Path rawRoot = AppPaths.RAW_DIR.toAbsolutePath().normalize();
        Path editedRoot = AppPaths.EDITED_DIR.toAbsolutePath().normalize();

        if (parent.equals(rawRoot) || parent.equals(editedRoot)) {
            return;
        }

        if (!parent.startsWith(rawRoot) && !parent.startsWith(editedRoot)) {
            return;
        }

        try {
            if (Files.exists(parent) && Files.isDirectory(parent)) {
                try (Stream<Path> stream = Files.list(parent)) {
                    if (stream.findAny().isEmpty()) {
                        Files.deleteIfExists(parent);
                    }
                }
            }
        } catch (IOException ignored) {
            // Không cần dừng tool nếu parent folder chưa xóa được.
        }
    }

    private record FolderCleanupResult(
            int deletedFolderCount,
            int deletedFileCount,
            String message
    ) {
    }

    private record DeleteOneFolderResult(
            int deletedFolderCount,
            int deletedFileCount,
            String status
    ) {
    }
}