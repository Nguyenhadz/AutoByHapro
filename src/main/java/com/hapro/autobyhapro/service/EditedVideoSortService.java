package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;
import com.hapro.autobyhapro.entity.EditedVideoSortItemResult;
import com.hapro.autobyhapro.entity.EditedVideoSortJobResult;
import com.hapro.autobyhapro.entity.EditedVideoTarget;
import com.hapro.autobyhapro.repository.VideoRepository;
import com.hapro.autobyhapro.util.FileNameUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EditedVideoSortService {

    private static final Pattern BATCH_CODE_DOUBLE_PATTERN =
            Pattern.compile("P\\d{3}__D\\d{6}__B\\d{3}");

    private static final Pattern BATCH_CODE_SINGLE_PATTERN =
            Pattern.compile("P\\d{3}_D\\d{6}_B\\d{3}");

    private static final Pattern BATCH_CODE_COMPACT_PATTERN =
            Pattern.compile("P\\d{3}D\\d{6}B\\d{3}");

    private static final int MAX_EDITED_TITLE_LENGTH = 120;
    private static final int FALLBACK_RANDOM_DIGIT_COUNT = 11;
    private static final int DUPLICATE_RANDOM_DIGIT_COUNT = 6;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final VideoRepository videoRepository = new VideoRepository();

    public EditedVideoSortJobResult sortEditedVideos() {
        try {
            Files.createDirectories(AppPaths.CAPCUT_EXPORT_DIR);
            Files.createDirectories(AppPaths.EDITED_DIR);
            Files.createDirectories(AppPaths.EDITED_UNKNOWN_DIR);
        } catch (IOException exception) {
            throw new RuntimeException("Không thể tạo folder cần thiết.", exception);
        }

        List<EditedVideoSortItemResult> results = new ArrayList<>();

        try (Stream<Path> stream = Files.list(AppPaths.CAPCUT_EXPORT_DIR)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .toList();

            for (Path file : files) {
                EditedVideoSortItemResult result = sortOneFile(file);
                results.add(result);
            }

            return new EditedVideoSortJobResult(results);

        } catch (IOException exception) {
            throw new RuntimeException("Không thể đọc folder capcut_export.", exception);
        }
    }

    private EditedVideoSortItemResult sortOneFile(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();

        if (!isVideoFile(fileName)) {
            return new EditedVideoSortItemResult(
                    sourceFile.toAbsolutePath().toString(),
                    null,
                    "",
                    "",
                    false,
                    "SKIPPED",
                    "Không phải file video nên bỏ qua."
            );
        }

        String batchCode = extractBatchCode(fileName);
        String videoIdOrPrefix = extractVideoIdOrPrefix(fileName);

        EditedVideoTarget target = null;

        if (!videoIdOrPrefix.isBlank()) {
            target = videoRepository.findEditedVideoTarget(
                    batchCode,
                    videoIdOrPrefix
            );
        }

        if (target == null) {
            target = videoRepository.findEditedVideoTargetByFileNameApprox(fileName);
        }

        if (target == null) {
            return moveToUnknown(
                    sourceFile,
                    videoIdOrPrefix,
                    batchCode,
                    "Không tìm thấy video phù hợp trong database. Có thể tên file bị CapCut cắt quá ngắn hoặc bị trùng gần đúng."
            );
        }

        try {
            Path editedFolder = Path.of(target.getEditedFolderPath());
            Files.createDirectories(editedFolder);

            String destinationFileName = buildDestinationFileName(
                    target,
                    fileName
            );

            Path destinationFile = resolveUniqueFile(
                    editedFolder.resolve(destinationFileName)
            );

            Files.move(
                    sourceFile,
                    destinationFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            videoRepository.saveEditedVideoFile(
                    target.getVideoId(),
                    destinationFile.toAbsolutePath().toString()
            );

            videoRepository.updateVideoBatchStatus(
                    target.getBatchId(),
                    "READY_UPLOAD"
            );

            return new EditedVideoSortItemResult(
                    sourceFile.toAbsolutePath().toString(),
                    destinationFile.toAbsolutePath().toString(),
                    target.getPlatformVideoId(),
                    target.getBatchCode(),
                    true,
                    "READY_UPLOAD",
                    "Đã phân loại video edit thành công và đổi tên theo tiêu đề nguồn."
            );

        } catch (Exception exception) {
            return new EditedVideoSortItemResult(
                    sourceFile.toAbsolutePath().toString(),
                    null,
                    videoIdOrPrefix,
                    batchCode,
                    false,
                    "FAILED",
                    exception.getMessage()
            );
        }
    }

    private String buildDestinationFileName(
            EditedVideoTarget target,
            String sourceFileName
    ) {
        String extension = getPreferredExtension(
                sourceFileName,
                target.getRawFilePath()
        );

        String safeTitle = buildSafeEditedTitle(target.getTitle());

        if (!safeTitle.isBlank()) {
            return safeTitle + extension;
        }

        return generateLongNumericName() + extension;
    }

    private String buildSafeEditedTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        String cleanTitle = title.trim();

        if ("unknown".equalsIgnoreCase(cleanTitle)
                || "untitled".equalsIgnoreCase(cleanTitle)
                || "null".equalsIgnoreCase(cleanTitle)
                || "none".equalsIgnoreCase(cleanTitle)) {
            return "";
        }

        String safeTitle = FileNameUtil.safeFileName(
                cleanTitle,
                MAX_EDITED_TITLE_LENGTH
        );

        if (safeTitle == null) {
            return "";
        }

        safeTitle = safeTitle.trim();

        while (safeTitle.endsWith(".") || safeTitle.endsWith(" ")) {
            safeTitle = safeTitle.substring(0, safeTitle.length() - 1).trim();
        }

        if (safeTitle.isBlank()) {
            return "";
        }

        if (isWindowsReservedFileName(safeTitle)) {
            safeTitle = "_" + safeTitle;
        }

        return safeTitle;
    }

    private boolean isWindowsReservedFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }

        String upperName = fileName.trim().toUpperCase();

        if (upperName.equals("CON")
                || upperName.equals("PRN")
                || upperName.equals("AUX")
                || upperName.equals("NUL")) {
            return true;
        }

        return upperName.matches("COM[1-9]")
                || upperName.matches("LPT[1-9]");
    }

    private String getPreferredExtension(
            String sourceFileName,
            String rawFilePath
    ) {
        String sourceExtension = getExtension(sourceFileName);

        if (!sourceExtension.isBlank()) {
            return sourceExtension;
        }

        if (rawFilePath != null && !rawFilePath.isBlank()) {
            String rawFileName = Path.of(rawFilePath)
                    .getFileName()
                    .toString();

            String rawExtension = getExtension(rawFileName);

            if (!rawExtension.isBlank()) {
                return rawExtension;
            }
        }

        return ".mp4";
    }

    private String generateLongNumericName() {
        return System.currentTimeMillis()
                + randomDigits(FALLBACK_RANDOM_DIGIT_COUNT);
    }

    private String randomDigits(int length) {
        StringBuilder builder = new StringBuilder(length);

        for (int index = 0; index < length; index++) {
            builder.append(SECURE_RANDOM.nextInt(10));
        }

        return builder.toString();
    }

    private EditedVideoSortItemResult moveToUnknown(
            Path sourceFile,
            String videoId,
            String batchCode,
            String message
    ) {
        try {
            Files.createDirectories(AppPaths.EDITED_UNKNOWN_DIR);

            Path destinationFile = resolveUniqueFile(
                    AppPaths.EDITED_UNKNOWN_DIR.resolve(sourceFile.getFileName())
            );

            Files.move(
                    sourceFile,
                    destinationFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return new EditedVideoSortItemResult(
                    sourceFile.toAbsolutePath().toString(),
                    destinationFile.toAbsolutePath().toString(),
                    videoId,
                    batchCode,
                    false,
                    "UNKNOWN",
                    message
            );

        } catch (Exception exception) {
            return new EditedVideoSortItemResult(
                    sourceFile.toAbsolutePath().toString(),
                    null,
                    videoId,
                    batchCode,
                    false,
                    "FAILED",
                    message + " Đồng thời không thể chuyển vào edited_unknown: " + exception.getMessage()
            );
        }
    }

    private String extractBatchCode(String fileName) {
        Matcher doubleMatcher = BATCH_CODE_DOUBLE_PATTERN.matcher(fileName);

        if (doubleMatcher.find()) {
            return doubleMatcher.group();
        }

        Matcher singleMatcher = BATCH_CODE_SINGLE_PATTERN.matcher(fileName);

        if (singleMatcher.find()) {
            return normalizeBatchCode(singleMatcher.group());
        }

        Matcher compactMatcher = BATCH_CODE_COMPACT_PATTERN.matcher(fileName);

        if (compactMatcher.find()) {
            return normalizeBatchCode(compactMatcher.group());
        }

        return "";
    }

    private String normalizeBatchCode(String batchCode) {
        if (batchCode == null || batchCode.isBlank()) {
            return "";
        }

        if (batchCode.matches("P\\d{3}__D\\d{6}__B\\d{3}")) {
            return batchCode;
        }

        if (batchCode.matches("P\\d{3}_D\\d{6}_B\\d{3}")) {
            return batchCode.replace("_D", "__D")
                    .replace("_B", "__B");
        }

        if (batchCode.matches("P\\d{3}D\\d{6}B\\d{3}")) {
            return batchCode.substring(0, 4)
                    + "__"
                    + batchCode.substring(4, 11)
                    + "__"
                    + batchCode.substring(11);
        }

        return batchCode;
    }

    private String extractVideoIdOrPrefix(String fileName) {
        String nameWithoutExtension = removeExtension(fileName);

        String[] markers = {
                "__VID_",
                "_VID_",
                "VID_"
        };

        for (String marker : markers) {
            int start = nameWithoutExtension.indexOf(marker);

            if (start >= 0) {
                start = start + marker.length();
                return extractVideoIdAfterMarker(nameWithoutExtension, start);
            }
        }

        return "";
    }

    private String extractVideoIdAfterMarker(String text, int start) {
        int end = findFirstDelimiter(
                text,
                start,
                "__B_",
                "_B_",
                "__N_",
                "_N_",
                "__TITLE_",
                "_TITLE_"
        );

        if (end < 0) {
            end = text.length();
        }

        if (end <= start) {
            return "";
        }

        return text.substring(start, end).trim();
    }

    private int findFirstDelimiter(String text, int start, String... delimiters) {
        int bestIndex = -1;

        for (String delimiter : delimiters) {
            int index = text.indexOf(delimiter, start);

            if (index < 0) {
                continue;
            }

            if (bestIndex < 0 || index < bestIndex) {
                bestIndex = index;
            }
        }

        return bestIndex;
    }

    private String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }

        return fileName;
    }

    private String getExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex);
        }

        return "";
    }

    private boolean isVideoFile(String fileName) {
        String lower = fileName.toLowerCase();

        return lower.endsWith(".mp4")
                || lower.endsWith(".mov")
                || lower.endsWith(".mkv")
                || lower.endsWith(".avi")
                || lower.endsWith(".webm");
    }

    private Path resolveUniqueFile(Path destinationFile) {
        if (!Files.exists(destinationFile)) {
            return destinationFile;
        }

        String fileName = destinationFile.getFileName().toString();
        String baseName = removeExtension(fileName);
        String extension = getExtension(fileName);
        Path parent = destinationFile.getParent();

        for (int index = 0; index < 1000; index++) {
            String numericSuffix = System.currentTimeMillis()
                    + randomDigits(DUPLICATE_RANDOM_DIGIT_COUNT);

            Path candidate = parent.resolve(
                    baseName + "_" + numericSuffix + extension
            );

            if (!Files.exists(candidate)) {
                return candidate;
            }
        }

        throw new RuntimeException(
                "Không thể tạo tên file không trùng trong folder edited: "
                        + parent.toAbsolutePath()
        );
    }
}
