package com.hapro.autobyhapro.license;

import com.hapro.autobyhapro.config.AppPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

public final class LicenseManager {

    private static final Path LICENSE_FILE =
            AppPaths.DATA_DIR.resolve("license.lic");

    private LicenseManager() {
    }

    public static LicenseCheckResult checkLicense() {
        try {
            if (!Files.exists(LICENSE_FILE)) {
                return LicenseCheckResult.invalid(
                        "NO_LICENSE",
                        "Tool chưa được kích hoạt licence."
                );
            }

            if (LicenseConstants.PUBLIC_KEY_BASE64 == null
                    || LicenseConstants.PUBLIC_KEY_BASE64.isBlank()) {
                return LicenseCheckResult.invalid(
                        "PUBLIC_KEY_MISSING",
                        "App chưa được cấu hình public key. Cần chạy admin key generator rồi dán public key vào LicenseConstants."
                );
            }

            String json = Files.readString(LICENSE_FILE, StandardCharsets.UTF_8);
            LicenseInfo licenseInfo = LicenseCryptoUtil.fromJson(json);

            if (licenseInfo.getAppCode() == null
                    || !LicenseConstants.APP_CODE.equals(licenseInfo.getAppCode())) {
                return LicenseCheckResult.invalid(
                        "WRONG_APP",
                        "Licence này không phải của auto-by-Hapro.",
                        licenseInfo
                );
            }

            boolean signatureValid = LicenseCryptoUtil.verifyLicenseInfo(
                    licenseInfo,
                    LicenseConstants.PUBLIC_KEY_BASE64
            );

            if (!signatureValid) {
                return LicenseCheckResult.invalid(
                        "INVALID_SIGNATURE",
                        "Licence không hợp lệ hoặc đã bị chỉnh sửa.",
                        licenseInfo
                );
            }

            String currentMachineId = MachineIdUtil.getMachineId();

            if (!currentMachineId.equalsIgnoreCase(licenseInfo.getMachineId())) {
                return LicenseCheckResult.invalid(
                        "WRONG_MACHINE",
                        "Licence này không thuộc máy tính hiện tại.",
                        licenseInfo
                );
            }

            if (isExpired(licenseInfo)) {
                return LicenseCheckResult.invalid(
                        "EXPIRED",
                        "Licence đã hết hạn.",
                        licenseInfo
                );
            }

            return LicenseCheckResult.valid(
                    licenseInfo,
                    "Licence hợp lệ."
            );

        } catch (Exception exception) {
            return LicenseCheckResult.invalid(
                    "ERROR",
                    "Không thể kiểm tra licence: " + exception.getMessage()
            );
        }
    }

    public static LicenseCheckResult importLicense(Path sourceFile) {
        if (sourceFile == null) {
            return LicenseCheckResult.invalid(
                    "NO_FILE",
                    "Chưa chọn file licence."
            );
        }

        try {
            Files.createDirectories(AppPaths.DATA_DIR);

            Files.copy(
                    sourceFile,
                    LICENSE_FILE,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return checkLicense();

        } catch (Exception exception) {
            return LicenseCheckResult.invalid(
                    "IMPORT_FAILED",
                    "Không thể import licence: " + exception.getMessage()
            );
        }
    }

    public static Path getLicenseFile() {
        return LICENSE_FILE;
    }

    private static boolean isExpired(LicenseInfo licenseInfo) {
        if (licenseInfo == null) {
            return true;
        }

        if (licenseInfo.isLifetime()) {
            return false;
        }

        String expiresAt = licenseInfo.getExpiresAt();

        if (expiresAt == null || expiresAt.isBlank()) {
            return true;
        }

        try {
            LocalDate expireDate = LocalDate.parse(expiresAt);
            LocalDate today = LocalDate.now();

            return today.isAfter(expireDate);

        } catch (Exception exception) {
            return true;
        }
    }
}