package com.hapro.autobyhapro.license;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LicenseCryptoUtil {

    private LicenseCryptoUtil() {
    }

    public static String toJson(LicenseInfo licenseInfo) {
        return "{\n"
                + "  \"appCode\": \"" + escape(licenseInfo.getAppCode()) + "\",\n"
                + "  \"machineId\": \"" + escape(licenseInfo.getMachineId()) + "\",\n"
                + "  \"licenseType\": \"" + escape(licenseInfo.getLicenseType()) + "\",\n"
                + "  \"customerName\": \"" + escape(licenseInfo.getCustomerName()) + "\",\n"
                + "  \"issuedAt\": \"" + escape(licenseInfo.getIssuedAt()) + "\",\n"
                + "  \"expiresAt\": \"" + escape(licenseInfo.getExpiresAt()) + "\",\n"
                + "  \"signature\": \"" + escape(licenseInfo.getSignature()) + "\"\n"
                + "}\n";
    }

    public static LicenseInfo fromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new RuntimeException("File licence đang trống.");
        }

        return new LicenseInfo(
                getString(json, "machineId"),
                getString(json, "licenseType"),
                getString(json, "customerName"),
                getString(json, "issuedAt"),
                getString(json, "expiresAt"),
                getString(json, "appCode"),
                getString(json, "signature")
        );
    }

    public static String signLicenseInfo(
            LicenseInfo licenseInfo,
            String privateKeyBase64
    ) {
        try {
            PrivateKey privateKey = readPrivateKey(privateKeyBase64);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(canonicalPayload(licenseInfo).getBytes(StandardCharsets.UTF_8));

            byte[] signedBytes = signature.sign();

            return Base64.getEncoder().encodeToString(signedBytes);

        } catch (Exception exception) {
            throw new RuntimeException("Không thể ký licence.", exception);
        }
    }

    public static boolean verifyLicenseInfo(
            LicenseInfo licenseInfo,
            String publicKeyBase64
    ) {
        try {
            PublicKey publicKey = readPublicKey(publicKeyBase64);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(canonicalPayload(licenseInfo).getBytes(StandardCharsets.UTF_8));

            byte[] signedBytes = Base64.getDecoder().decode(cleanBase64(licenseInfo.getSignature()));

            return signature.verify(signedBytes);

        } catch (Exception exception) {
            return false;
        }
    }

    public static String readKeyBase64FromFile(Path keyFile) {
        try {
            String text = Files.readString(keyFile, StandardCharsets.UTF_8);
            return cleanBase64(text);

        } catch (Exception exception) {
            throw new RuntimeException("Không thể đọc key file: " + keyFile, exception);
        }
    }

    private static String canonicalPayload(LicenseInfo licenseInfo) {
        return safe(licenseInfo.getAppCode())
                + "\n" + safe(licenseInfo.getMachineId())
                + "\n" + safe(licenseInfo.getLicenseType())
                + "\n" + safe(licenseInfo.getCustomerName())
                + "\n" + safe(licenseInfo.getIssuedAt())
                + "\n" + safe(licenseInfo.getExpiresAt());
    }

    private static PrivateKey readPrivateKey(String privateKeyBase64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(cleanBase64(privateKeyBase64));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);

        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private static PublicKey readPublicKey(String publicKeyBase64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(cleanBase64(publicKeyBase64));
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);

        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private static String getString(String json, String fieldName) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\""
        );

        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            return "";
        }

        return unescape(matcher.group(1));
    }

    private static String cleanBase64(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static String unescape(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static String safe(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }
}
