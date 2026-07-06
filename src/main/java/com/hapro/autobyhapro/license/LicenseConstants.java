package com.hapro.autobyhapro.license;

public final class LicenseConstants {

    public static final String APP_CODE = "AUTO_BY_HAPRO";

    /*
     * Sau khi chạy LicenseAdminKeyGenerator, m copy nội dung trong:
     * license_admin/public_key_base64.txt
     * rồi dán vào đây.
     *
     * Chỉ public key được để trong app.
     * Private key tuyệt đối không copy vào bản release cho khách.
     */
    public static final String PUBLIC_KEY_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl5GOromVeIDOKXb1+ECf0w6fxhAzXzZ2GXXBMZnEBjrgMIJ37qz8O/sCeJey58+sS/JWMaQLgoUV3QpiZwdDHJuEuA1wq6MUuWxokbvYUta/3+30xpv5anzyVBYLfZkxbSG7a4y6Gvo65Uhcwo3xKwNxgvHNNYZA50myJLkb2Wxx0LqUHag5tGI0wI1ycIrWdkVhI/Bhjuw69ynptCYAH+ZzV08ZQMSkVBjB5cVBKl8XlrPxIx66maPS0JNyPv24tXBhU+Z1HngYZmfA3cYDjCK/2UBGcEnJvom2bbWlBwd1L9So5FWjUlPi2OgCPNr61LEfGMRojyaNctXI4bOThwIDAQAB";

    private LicenseConstants() {
    }
}