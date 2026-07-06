package com.hapro.autobyhapro.entity;

public class CsvExportResult {

    private final String filePath;
    private final int rowCount;
    private final String message;

    public CsvExportResult(
            String filePath,
            int rowCount,
            String message
    ) {
        this.filePath = filePath;
        this.rowCount = rowCount;
        this.message = message;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getRowCount() {
        return rowCount;
    }

    public String getMessage() {
        return message;
    }
}
