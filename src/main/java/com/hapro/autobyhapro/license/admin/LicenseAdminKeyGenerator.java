package com.hapro.autobyhapro.license.admin;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class LicenseAdminKeyGenerator {

    private static final Path ADMIN_DIR = Path.of("license_admin");
    private static final Path PRIVATE_KEY_FILE = ADMIN_DIR.resolve("private_key.pem");
    private static final Path PUBLIC_KEY_FILE = ADMIN_DIR.resolve("public_key.pem");
    private static final Path PUBLIC_KEY_BASE64_FILE = ADMIN_DIR.resolve("public_key_base64.txt");

    public static void main(String[] args) {
        try {
            Files.createDirectories(ADMIN_DIR);

            if (Files.exists(PRIVATE_KEY_FILE)) {
                System.out.println("Đã tồn tại private key:");
                System.out.println(PRIVATE_KEY_FILE.toAbsolutePath());
                System.out.println();
                System.out.println("Để tránh mất quyền tạo licence cũ, tool sẽ không ghi đè key.");
                System.out.println("Nếu thật sự muốn tạo key mới, hãy tự xóa folder license_admin trước.");
                return;
            }

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);

            KeyPair keyPair = generator.generateKeyPair();

            String privateBase64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                    .encodeToString(keyPair.getPrivate().getEncoded());

            String publicBase64 = Base64.getEncoder()
                    .encodeToString(keyPair.getPublic().getEncoded());

            String publicPem = "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                    .encodeToString(keyPair.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----\n";

            String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                    + privateBase64
                    + "\n-----END PRIVATE KEY-----\n";

            Files.writeString(PRIVATE_KEY_FILE, privatePem, StandardCharsets.UTF_8);
            Files.writeString(PUBLIC_KEY_FILE, publicPem, StandardCharsets.UTF_8);
            Files.writeString(PUBLIC_KEY_BASE64_FILE, publicBase64, StandardCharsets.UTF_8);

            System.out.println("Đã tạo key licence thành công.");
            System.out.println();
            System.out.println("PRIVATE KEY - giữ riêng, không đưa cho khách:");
            System.out.println(PRIVATE_KEY_FILE.toAbsolutePath());
            System.out.println();
            System.out.println("PUBLIC KEY - copy nội dung file này vào LicenseConstants.PUBLIC_KEY_BASE64:");
            System.out.println(PUBLIC_KEY_BASE64_FILE.toAbsolutePath());

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
