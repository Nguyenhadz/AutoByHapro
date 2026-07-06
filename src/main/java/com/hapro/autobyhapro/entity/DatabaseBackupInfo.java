package com.hapro.autobyhapro.entity;

public class DatabaseBackupInfo {

    private final String fileName;
    private final String filePath;
    private final long fileSizeBytes;
    private final String lastModifiedTime;

    public DatabaseBackupInfo(
            String fileName,
            String filePath,
            long fileSizeBytes,
            String lastModifiedTime
    ) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSizeBytes = fileSizeBytes;
        this.lastModifiedTime = lastModifiedTime;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getLastModifiedTime() {
        return lastModifiedTime;
    }
}
