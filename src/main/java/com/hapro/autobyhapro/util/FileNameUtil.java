package com.hapro.autobyhapro.util;

import java.text.Normalizer;

public class FileNameUtil {

    private static final int DEFAULT_MAX_LENGTH = 120;

    private FileNameUtil() {
    }

    public static String safeFileName(String text) {
        return safeFileName(text, DEFAULT_MAX_LENGTH);
    }

    public static String safeFileName(String text, int maxLength) {
        return cleanForWindowsFileName(text, maxLength);
    }

    public static String safeFolderName(String text, int maxLength) {
        return cleanForWindowsFileName(text, maxLength);
    }

    public static String safeFolderName(String text) {
        return safeFolderName(text, DEFAULT_MAX_LENGTH);
    }

    public static String cleanFileName(String text, int maxLength) {
        return cleanForWindowsFileName(text, maxLength);
    }

    public static String cleanFileName(String text) {
        return cleanForWindowsFileName(text, DEFAULT_MAX_LENGTH);
    }

    public static String cleanTitle(String text, int maxLength) {
        return cleanForWindowsFileName(text, maxLength);
    }

    public static String cleanTitle(String text) {
        return cleanForWindowsFileName(text, DEFAULT_MAX_LENGTH);
    }

    public static String capitalizeWords(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String cleanText = text.trim().replaceAll("\\s+", " ");
        String[] words = cleanText.split(" ");

        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(" ");
            }

            if (word.length() == 1) {
                builder.append(word.toUpperCase());
            } else {
                String firstChar = word.substring(0, 1).toUpperCase();
                String rest = word.substring(1).toLowerCase();

                builder.append(firstChar).append(rest);
            }
        }

        return builder.toString();
    }

    public static String capitalizeWords(String text, int maxLength) {
        String result = capitalizeWords(text);
        return cleanForWindowsFileName(result, maxLength);
    }

    private static String cleanForWindowsFileName(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "UNTITLED";
        }

        String result = text.trim();

        // Giữ tiếng Việt có dấu ở dạng chuẩn Unicode.
        result = Normalizer.normalize(result, Normalizer.Form.NFC);

        // Xóa ký tự lỗi replacement char nếu title bị lỗi decode từ trước.
        result = result.replace("�", "");

        // Các ký tự Windows không cho dùng trong tên file/folder.
        result = result.replaceAll("[<>:\"/\\\\|?*]", " ");

        // Xóa control character.
        result = result.replaceAll("[\\p{Cntrl}]", " ");

        // Không xóa chữ tiếng Việt. Chỉ gom khoảng trắng.
        result = result.replaceAll("\\s+", " ").trim();

        while (result.endsWith(".") || result.endsWith(" ")) {
            result = result.substring(0, result.length() - 1).trim();
        }

        if (result.isBlank()) {
            result = "UNTITLED";
        }

        result = avoidWindowsReservedName(result);

        if (maxLength > 0 && result.length() > maxLength) {
            result = result.substring(0, maxLength).trim();

            while (result.endsWith(".") || result.endsWith(" ")) {
                result = result.substring(0, result.length() - 1).trim();
            }

            if (result.isBlank()) {
                result = "UNTITLED";
            }
        }

        return result;
    }

    private static String avoidWindowsReservedName(String name) {
        String upperName = name.toUpperCase();

        if (upperName.equals("CON")
                || upperName.equals("PRN")
                || upperName.equals("AUX")
                || upperName.equals("NUL")
                || upperName.equals("COM1")
                || upperName.equals("COM2")
                || upperName.equals("COM3")
                || upperName.equals("COM4")
                || upperName.equals("COM5")
                || upperName.equals("COM6")
                || upperName.equals("COM7")
                || upperName.equals("COM8")
                || upperName.equals("COM9")
                || upperName.equals("LPT1")
                || upperName.equals("LPT2")
                || upperName.equals("LPT3")
                || upperName.equals("LPT4")
                || upperName.equals("LPT5")
                || upperName.equals("LPT6")
                || upperName.equals("LPT7")
                || upperName.equals("LPT8")
                || upperName.equals("LPT9")) {
            return "_" + name;
        }

        return name;
    }
}