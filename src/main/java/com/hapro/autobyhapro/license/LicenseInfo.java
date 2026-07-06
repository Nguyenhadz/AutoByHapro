package com.hapro.autobyhapro.license;

public class LicenseInfo {

    private final String machineId;
    private final String licenseType;
    private final String customerName;
    private final String issuedAt;
    private final String expiresAt;
    private final String appCode;
    private final String signature;

    public LicenseInfo(
            String machineId,
            String licenseType,
            String customerName,
            String issuedAt,
            String expiresAt,
            String appCode,
            String signature
    ) {
        this.machineId = machineId;
        this.licenseType = licenseType;
        this.customerName = customerName;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.appCode = appCode;
        this.signature = signature;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public String getAppCode() {
        return appCode;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isLifetime() {
        return "LIFETIME".equalsIgnoreCase(licenseType);
    }
}
