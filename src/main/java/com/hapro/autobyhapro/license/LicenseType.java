package com.hapro.autobyhapro.license;

public enum LicenseType {

    MONTH_1("30 ngày", 30),
    YEAR_1("1 năm", 365),
    LIFETIME("Vĩnh viễn", -1);

    private final String displayName;
    private final int validDays;

    LicenseType(String displayName, int validDays) {
        this.displayName = displayName;
        this.validDays = validDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getValidDays() {
        return validDays;
    }

    public boolean isLifetime() {
        return this == LIFETIME;
    }

    public static LicenseType fromInput(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Loại licence đang bị trống.");
        }

        String clean = input.trim().toUpperCase();

        return switch (clean) {
            case "1", "30", "30D", "MONTH", "MONTH_1" -> MONTH_1;
            case "2", "365", "1Y", "YEAR", "YEAR_1" -> YEAR_1;
            case "3", "FOREVER", "LIFE", "LIFETIME", "VINHVIEN", "VĨNH VIỄN" -> LIFETIME;
            default -> throw new IllegalArgumentException("Loại licence không hợp lệ: " + input);
        };
    }
}