package com.hapro.autobyhapro.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.file.DirectoryStream;
import java.nio.file.StandardCopyOption;

public final class AppPaths {
    private static final Path ROOT_DIR = Paths.get(System.getProperty("user.dir"));

    public static final Path DATA_DIR = ROOT_DIR.resolve("data");

    public static final Path COOKIES_DIR = DATA_DIR.resolve("cookies");
    public static final Path YOUTUBE_COOKIES_FILE = COOKIES_DIR.resolve("youtube_cookies.txt");
    public static final Path TOOLS_DIR = ROOT_DIR.resolve("tools");
    public static final Path VIDEO_DIR = ROOT_DIR.resolve("video");

    public static final Path RAW_DIR = VIDEO_DIR.resolve("raw");
    public static final Path CAPCUT_EXPORT_DIR = VIDEO_DIR.resolve("capcut_export");
    public static final Path EDITED_DIR = VIDEO_DIR.resolve("edited");
    public static final Path EDITED_UNKNOWN_DIR = VIDEO_DIR.resolve("edited_unknown");
    public static final Path BACKUP_DATABASE_DIR = ROOT_DIR.resolve("backups").resolve("database");
    public static final Path LOGS_DIR = ROOT_DIR.resolve("logs");
    public static final Path EXPORTS_DIR = ROOT_DIR.resolve("exports");

    private AppPaths() {
    }

    public static Path rootDir() {
        return ROOT_DIR;
    }

    public static Path databaseFile() {
        return DATA_DIR.resolve("download.db");
    }

    public static Path ytDlpFile() {
        return TOOLS_DIR.resolve("yt-dlp.exe");
    }

    public static Path denoFile() {
        return TOOLS_DIR.resolve("deno").resolve("deno.exe");
    }

    public static Path ffmpegFile() {
        return TOOLS_DIR.resolve("ffmpeg").resolve("bin").resolve("ffmpeg.exe");
    }

    public static void ensureBaseDirectories() throws IOException {
        Files.createDirectories(DATA_DIR);
        Files.createDirectories(COOKIES_DIR);
        Files.createDirectories(TOOLS_DIR);
        Files.createDirectories(VIDEO_DIR);
        Files.createDirectories(RAW_DIR);
        Files.createDirectories(CAPCUT_EXPORT_DIR);
        Files.createDirectories(EDITED_DIR);
        Files.createDirectories(EDITED_UNKNOWN_DIR);
        Files.createDirectories(BACKUP_DATABASE_DIR);
        Files.createDirectories(LOGS_DIR);
        Files.createDirectories(EXPORTS_DIR);

        migrateOldVideoFolders();
    }
    private static void migrateOldVideoFolders() {
        moveOldFolderToVideo("raw", RAW_DIR);
        moveOldFolderToVideo("capcut_export", CAPCUT_EXPORT_DIR);
        moveOldFolderToVideo("edited", EDITED_DIR);
        moveOldFolderToVideo("edited_unknown", EDITED_UNKNOWN_DIR);
    }

    private static void moveOldFolderToVideo(String oldFolderName, Path newFolder) {
        Path oldFolder = ROOT_DIR.resolve(oldFolderName);

        try {
            if (!Files.exists(oldFolder) || !Files.isDirectory(oldFolder)) {
                return;
            }

            Files.createDirectories(newFolder);
            moveFolderContent(oldFolder, newFolder);

            try {
                Files.deleteIfExists(oldFolder);
            } catch (Exception ignored) {
                // Nếu folder cũ chưa rỗng hoặc đang bị Windows giữ file thì bỏ qua, không làm crash app.
            }

        } catch (Exception exception) {
            System.out.println("Không thể chuyển folder cũ " + oldFolderName + " vào video: " + exception.getMessage());
        }
    }

    private static void moveFolderContent(Path sourceFolder, Path targetFolder) throws IOException {
        Files.createDirectories(targetFolder);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceFolder)) {
            for (Path sourcePath : stream) {
                Path targetPath = targetFolder.resolve(sourcePath.getFileName().toString());

                if (Files.isDirectory(sourcePath)) {
                    moveFolderContent(sourcePath, targetPath);

                    try {
                        Files.deleteIfExists(sourcePath);
                    } catch (Exception ignored) {
                    }

                } else {
                    Path finalTarget = makeUniqueTargetPath(targetPath);
                    Files.move(sourcePath, finalTarget, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Path makeUniqueTargetPath(Path targetPath) {
        if (!Files.exists(targetPath)) {
            return targetPath;
        }

        String fileName = targetPath.getFileName().toString();
        String name = fileName;
        String extension = "";

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            name = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        Path parent = targetPath.getParent();

        for (int index = 1; index <= 9999; index++) {
            Path candidate = parent.resolve(name + "_copy_" + index + extension);

            if (!Files.exists(candidate)) {
                return candidate;
            }
        }

        return targetPath;
    }
}