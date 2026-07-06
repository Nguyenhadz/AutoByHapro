package com.hapro.autobyhapro.license;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public final class MachineIdUtil {

    private static final long POWERSHELL_TIMEOUT_MILLIS = 4000;
    private static String cachedMachineId;

    private MachineIdUtil() {
    }

    public static String getMachineId() {
        if (cachedMachineId != null && !cachedMachineId.isBlank()) {
            return cachedMachineId;
        }

        String uuid = readPowerShellValue("(Get-CimInstance Win32_ComputerSystemProduct).UUID");
        String baseboard = readPowerShellValue("(Get-CimInstance Win32_BaseBoard).SerialNumber");
        String bios = readPowerShellValue("(Get-CimInstance Win32_BIOS).SerialNumber");

        String computerName = safe(System.getenv("COMPUTERNAME"));
        String processor = safe(System.getenv("PROCESSOR_IDENTIFIER"));
        String os = safe(System.getProperty("os.name"));
        String user = safe(System.getProperty("user.name"));

        String raw = LicenseConstants.APP_CODE
                + "|UUID=" + uuid
                + "|BOARD=" + baseboard
                + "|BIOS=" + bios
                + "|PC=" + computerName
                + "|CPU=" + processor
                + "|OS=" + os
                + "|USER=" + user;

        String hash = sha256(raw).toUpperCase(Locale.ROOT);

        cachedMachineId = "HAPRO-"
                + hash.substring(0, 8)
                + "-"
                + hash.substring(8, 16)
                + "-"
                + hash.substring(16, 24)
                + "-"
                + hash.substring(24, 32);

        return cachedMachineId;
    }

    private static String readPowerShellValue(String command) {
        Process process = null;

        try {
            process = new ProcessBuilder(
                    "powershell",
                    "-NoProfile",
                    "-Command",
                    command
            )
                    .redirectErrorStream(true)
                    .start();

            boolean finished = waitProcessWithTimeout(process, POWERSHELL_TIMEOUT_MILLIS);

            if (!finished) {
                process.destroyForcibly();
                return "";
            }

            StringBuilder builder = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;

                while ((line = reader.readLine()) != null) {
                    String clean = line.trim();

                    if (clean.isBlank()) {
                        continue;
                    }

                    String lower = clean.toLowerCase(Locale.ROOT);

                    if (lower.contains("serialnumber")) {
                        continue;
                    }

                    if (lower.contains("uuid")) {
                        continue;
                    }

                    builder.append(clean);
                }
            }

            return builder.toString().trim();

        } catch (Exception exception) {
            if (process != null) {
                process.destroyForcibly();
            }

            return "";
        }
    }

    private static boolean waitProcessWithTimeout(Process process, long timeoutMillis) {
        long startTime = System.currentTimeMillis();

        while (process.isAlive()) {
            long elapsed = System.currentTimeMillis() - startTime;

            if (elapsed >= timeoutMillis) {
                return false;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return true;
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder();

            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }

            return builder.toString();

        } catch (Exception exception) {
            throw new RuntimeException("Không thể tạo mã máy.", exception);
        }
    }

    private static String safe(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }
}