package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.DatabaseBackupInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class DatabaseBackupService {

    private static final DateTimeFormatter BACKUP_FILE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DatabaseBackupInfo backupNow() {
        try {
            Files.createDirectories(AppPaths.DATA_DIR);
            Files.createDirectories(AppPaths.BACKUP_DATABASE_DIR);

            Path databaseFile = AppPaths.databaseFile();

            if (!Files.exists(databaseFile)) {
                throw new RuntimeException("Không tìm thấy database: " + databaseFile.toAbsolutePath());
            }

            String backupFileName = "download_backup_"
                    + LocalDateTime.now().format(BACKUP_FILE_TIME_FORMAT)
                    + ".db";

            Path backupFile = AppPaths.BACKUP_DATABASE_DIR.resolve(backupFileName);

            Files.copy(
                    databaseFile,
                    backupFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            deleteOldBackupsExcept(backupFile);

            return toBackupInfo(backupFile);

        } catch (IOException exception) {
            throw new RuntimeException("Không thể backup database.", exception);
        }
    }

    public List<DatabaseBackupInfo> listBackups() {
        try {
            Files.createDirectories(AppPaths.BACKUP_DATABASE_DIR);

            try (Stream<Path> stream = Files.list(AppPaths.BACKUP_DATABASE_DIR)) {
                return stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".db"))
                        .sorted(Comparator.comparingLong(this::lastModifiedMillis).reversed())
                        .limit(1)
                        .map(this::toBackupInfo)
                        .toList();
            }

        } catch (IOException exception) {
            throw new RuntimeException("Không thể đọc folder backup database.", exception);
        }
    }

    public String restoreFromBackup(Path backupFile) {
        if (backupFile == null) {
            throw new RuntimeException("File backup đang bị trống.");
        }

        return restoreFromDatabaseFile(backupFile);
    }

    public String restoreFromExternalDatabaseFile(String externalPathText) {
        if (externalPathText == null || externalPathText.isBlank()) {
            throw new RuntimeException("Đường dẫn file DB đang bị trống.");
        }

        String cleanPath = externalPathText.trim();

        if (cleanPath.startsWith("\"") && cleanPath.endsWith("\"") && cleanPath.length() > 1) {
            cleanPath = cleanPath.substring(1, cleanPath.length() - 1);
        }

        Path externalFile = Path.of(cleanPath);

        return restoreFromDatabaseFile(externalFile);
    }

    private void deleteOldBackupsExcept(Path keepFile) {
        try {
            Path keep = keepFile.toAbsolutePath().normalize();

            try (Stream<Path> stream = Files.list(AppPaths.BACKUP_DATABASE_DIR)) {
                List<Path> oldBackups = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".db"))
                        .filter(path -> !path.toAbsolutePath().normalize().equals(keep))
                        .toList();

                for (Path oldBackup : oldBackups) {
                    Files.deleteIfExists(oldBackup);
                }
            }

        } catch (IOException exception) {
            throw new RuntimeException("Đã tạo backup mới nhưng không xóa được backup cũ.", exception);
        }
    }

    private String restoreFromDatabaseFile(Path sourceDatabaseFile) {
        try {
            Path source = sourceDatabaseFile.toAbsolutePath().normalize();

            if (!Files.exists(source)) {
                throw new RuntimeException("Không tìm thấy file DB: " + source);
            }

            if (!Files.isRegularFile(source)) {
                throw new RuntimeException("Đường dẫn này không phải file DB: " + source);
            }

            if (!source.getFileName().toString().toLowerCase().endsWith(".db")) {
                throw new RuntimeException("File khôi phục phải có đuôi .db");
            }

            Files.createDirectories(AppPaths.DATA_DIR);
            Files.createDirectories(AppPaths.BACKUP_DATABASE_DIR);

            Path currentDatabase = AppPaths.databaseFile().toAbsolutePath().normalize();

            String beforeRestoreBackupName = "download_before_restore_"
                    + LocalDateTime.now().format(BACKUP_FILE_TIME_FORMAT)
                    + ".db";

            Path beforeRestoreBackup = AppPaths.BACKUP_DATABASE_DIR.resolve(beforeRestoreBackupName);

            if (Files.exists(currentDatabase)) {
                Files.copy(
                        currentDatabase,
                        beforeRestoreBackup,
                        StandardCopyOption.REPLACE_EXISTING
                );

                deleteOldBackupsExcept(beforeRestoreBackup);
            }

            Files.copy(
                    source,
                    currentDatabase,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "Khôi phục DB thành công. DB cũ đã được backup trước khi restore tại: "
                    + beforeRestoreBackup.toAbsolutePath();

        } catch (IOException exception) {
            throw new RuntimeException("Không thể khôi phục database.", exception);
        }
    }

    private DatabaseBackupInfo toBackupInfo(Path file) {
        try {
            long size = Files.size(file);
            long modifiedMillis = lastModifiedMillis(file);

            String modifiedText = LocalDateTime
                    .ofInstant(Instant.ofEpochMilli(modifiedMillis), ZoneId.systemDefault())
                    .format(DISPLAY_TIME_FORMAT);

            return new DatabaseBackupInfo(
                    file.getFileName().toString(),
                    file.toAbsolutePath().toString(),
                    size,
                    modifiedText
            );

        } catch (IOException exception) {
            return new DatabaseBackupInfo(
                    file.getFileName().toString(),
                    file.toAbsolutePath().toString(),
                    0,
                    ""
            );
        }
    }

    private long lastModifiedMillis(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }
}