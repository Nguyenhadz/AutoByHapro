package com.hapro.autobyhapro.license;

public class LicenseCheckResult {

    private final boolean valid;
    private final String status;
    private final String message;
    private final LicenseInfo licenseInfo;

    private LicenseCheckResult(
            boolean valid,
            String status,
            String message,
            LicenseInfo licenseInfo
    ) {
        this.valid = valid;
        this.status = status;
        this.message = message;
        this.licenseInfo = licenseInfo;
    }

    public static LicenseCheckResult valid(LicenseInfo licenseInfo, String message) {
        return new LicenseCheckResult(true, "VALID", message, licenseInfo);
    }

    public static LicenseCheckResult invalid(String status, String message) {
        return new LicenseCheckResult(false, status, message, null);
    }

    public static LicenseCheckResult invalid(String status, String message, LicenseInfo licenseInfo) {
        return new LicenseCheckResult(false, status, message, licenseInfo);
    }

    public boolean isValid() {
        return valid;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LicenseInfo getLicenseInfo() {
        return licenseInfo;
    }
}
