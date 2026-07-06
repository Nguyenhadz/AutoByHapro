package com.hapro.autobyhapro.entity;

public class FanpageDeleteResult {

    private final Long fanpageId;
    private final String pageCode;
    private final String pageName;
    private final int databaseDeletedRows;
    private final int deletedFolderCount;
    private final int deletedFileCount;
    private final String message;

    public FanpageDeleteResult(
            Long fanpageId,
            String pageCode,
            String pageName,
            int databaseDeletedRows,
            int deletedFolderCount,
            int deletedFileCount,
            String message
    ) {
        this.fanpageId = fanpageId;
        this.pageCode = pageCode;
        this.pageName = pageName;
        this.databaseDeletedRows = databaseDeletedRows;
        this.deletedFolderCount = deletedFolderCount;
        this.deletedFileCount = deletedFileCount;
        this.message = message;
    }

    public Long getFanpageId() {
        return fanpageId;
    }

    public String getPageCode() {
        return pageCode;
    }

    public String getPageName() {
        return pageName;
    }

    public int getDatabaseDeletedRows() {
        return databaseDeletedRows;
    }

    public int getDeletedFolderCount() {
        return deletedFolderCount;
    }

    public int getDeletedFileCount() {
        return deletedFileCount;
    }

    public String getMessage() {
        return message;
    }
}