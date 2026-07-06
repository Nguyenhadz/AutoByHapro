package com.hapro.autobyhapro.license.admin;

import com.hapro.autobyhapro.license.LicenseConstants;
import com.hapro.autobyhapro.license.LicenseCryptoUtil;
import com.hapro.autobyhapro.license.LicenseInfo;
import com.hapro.autobyhapro.license.LicenseType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Scanner;

public class LicenseAdminGenerator {

    private static final Path PRIVATE_KEY_FILE =
            Path.of("license_admin").resolve("private_key.pem");

    private static final Path OUTPUT_DIR =
            Path.of("generated_licenses");

    public static void main(String[] args) {
        try {
            if (!Files.exists(PRIVATE_KEY_FILE)) {
                System.out.println("Chưa có private key.");
                System.out.println("Hãy chạy class này trước:");
                System.out.println("com.hapro.autobyhapro.license.admin.LicenseAdminKeyGenerator");
                return;
            }

            Files.createDirectories(OUTPUT_DIR);

            Scanner scanner = new Scanner(System.in);

            System.out.println("========== AUTO BY HAPRO - LICENSE GENERATOR ==========");
            System.out.println();

            System.out.print("Nhập mã máy khách gửi: ");
            String machineId = scanner.nextLine().trim();

            System.out.print("Nhập tên khách hàng: ");
            String customerName = scanner.nextLine().trim();

            System.out.println();
            System.out.println("Chọn loại licence:");
            System.out.println("1 = 30 ngày");
            System.out.println("2 = 1 năm");
            System.out.println("3 = Vĩnh viễn");
            System.out.print("Nhập lựa chọn: ");

            String typeInput = scanner.nextLine().trim();
            LicenseType licenseType = LicenseType.fromInput(typeInput);

            LocalDate issuedAt = LocalDate.now();
            LocalDate expiresAt;

            if (licenseType.isLifetime()) {
                expiresAt = LocalDate.of(9999, 12, 31);
            } else {
                expiresAt = issuedAt.plusDays(licenseType.getValidDays());
            }

            LicenseInfo unsignedInfo = new LicenseInfo(
                    machineId,
                    licenseType.name(),
                    customerName,
                    issuedAt.toString(),
                    expiresAt.toString(),
                    LicenseConstants.APP_CODE,
                    ""
            );

            String privateKeyBase64 = LicenseCryptoUtil.readKeyBase64FromFile(PRIVATE_KEY_FILE);
            String signature = LicenseCryptoUtil.signLicenseInfo(unsignedInfo, privateKeyBase64);

            LicenseInfo signedInfo = new LicenseInfo(
                    machineId,
                    licenseType.name(),
                    customerName,
                    issuedAt.toString(),
                    expiresAt.toString(),
                    LicenseConstants.APP_CODE,
                    signature
            );

            String safeMachine = machineId.replaceAll("[^A-Za-z0-9_-]", "_");
            String fileName = "license_"
                    + safeMachine
                    + "_"
                    + licenseType.name()
                    + ".lic";

            Path outputFile = OUTPUT_DIR.resolve(fileName);

            Files.writeString(
                    outputFile,
                    LicenseCryptoUtil.toJson(signedInfo),
                    StandardCharsets.UTF_8
            );

            System.out.println();
            System.out.println("Đã tạo licence thành công:");
            System.out.println(outputFile.toAbsolutePath());
            System.out.println();
            System.out.println("Loại licence: " + licenseType.getDisplayName());
            System.out.println("Ngày cấp: " + issuedAt);
            System.out.println("Ngày hết hạn: " + (licenseType.isLifetime() ? "Vĩnh viễn" : expiresAt));
            System.out.println();
            System.out.println("Gửi file .lic này cho khách để import vào tool.");

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}