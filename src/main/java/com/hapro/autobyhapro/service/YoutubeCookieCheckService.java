package com.hapro.autobyhapro.service;

import com.hapro.autobyhapro.config.AppPaths;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class YoutubeCookieCheckService {

    private static final int CHECK_TIMEOUT_SECONDS = 120;

    private static final String TEST_VIDEO_URL =
            "https://www.youtube.com/shorts/VrPDfTOtZIk";

    public String checkYoutubeCookies() {
        Path ytDlp = AppPaths.ytDlpFile();
        Path cookiesFile = AppPaths.YOUTUBE_COOKIES_FILE;
        Path denoFile = AppPaths.denoFile();

        StringBuilder result = new StringBuilder();

        result.append("KIỂM TRA COOKIES YOUTUBE")
                .append(System.lineSeparator());
        result.append("========================")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        result.append("yt-dlp: ")
                .append(ytDlp.toAbsolutePath())
                .append(System.lineSeparator());

        result.append("Cookies: ")
                .append(cookiesFile.toAbsolutePath())
                .append(System.lineSeparator());

        result.append("Deno: ")
                .append(denoFile.toAbsolutePath())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        if (!Files.exists(ytDlp)) {
            return result
                    + "KẾT QUẢ: LỖI\n\n"
                    + "Chưa có yt-dlp.exe.\n"
                    + "Hãy kiểm tra lại folder tools.";
        }

        if (!Files.exists(cookiesFile)) {
            return result
                    + "KẾT QUẢ: LỖI\n\n"
                    + "Chưa có file cookies YouTube.\n\n"
                    + "Cần đặt file tại:\n"
                    + cookiesFile.toAbsolutePath()
                    + "\n\nTên file bắt buộc:\n"
                    + "youtube_cookies.txt";
        }

        if (!Files.exists(denoFile)) {
            return result
                    + "KẾT QUẢ: LỖI\n\n"
                    + "Chưa có deno.exe.\n\n"
                    + "Cần đặt tại:\n"
                    + denoFile.toAbsolutePath()
                    + "\n\nDeno dùng để yt-dlp giải YouTube JS challenge.";
        }

        List<String> command = buildCheckCommand(ytDlp, cookiesFile);

        CommandResult commandResult = runCommand(command, CHECK_TIMEOUT_SECONDS);

        result.append("Exit code: ")
                .append(commandResult.exitCode())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        String output = commandResult.output();

        if (!commandResult.success()) {
            result.append("KẾT QUẢ: LỖI")
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());

            result.append(explainYoutubeCookieError(output))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());

            result.append("Output:")
                    .append(System.lineSeparator())
                    .append(output);

            return result.toString();
        }

        if (isCookieOk(output)) {
            result.append("KẾT QUẢ: OK")
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());

            result.append("Cookies YouTube dùng được.")
                    .append(System.lineSeparator());

            result.append("yt-dlp đã lấy được format video/audio thật.")
                    .append(System.lineSeparator());

            result.append("Có thể bắt đầu download YouTube.")
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());

            result.append("Output rút gọn:")
                    .append(System.lineSeparator())
                    .append(shortenOutput(output, 80));

            return result.toString();
        }

        result.append("KẾT QUẢ: CHƯA OK")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        result.append(explainYoutubeCookieError(output))
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        result.append("Output:")
                .append(System.lineSeparator())
                .append(output);

        return result.toString();
    }

    private List<String> buildCheckCommand(Path ytDlp, Path cookiesFile) {
        List<String> command = new ArrayList<>();

        command.add(ytDlp.toAbsolutePath().toString());

        command.add("--encoding");
        command.add("utf-8");

        command.add("--cookies");
        command.add(cookiesFile.toAbsolutePath().toString());

        command.add("--remote-components");
        command.add("ejs:github");

        command.add("--socket-timeout");
        command.add("20");

        command.add("-F");
        command.add(TEST_VIDEO_URL);

        return command;
    }

    private boolean isCookieOk(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }

        String lowerOutput = output.toLowerCase();

        boolean hasAvailableFormats =
                lowerOutput.contains("available formats");

        boolean hasVideoOrAudioFormat =
                lowerOutput.contains(" mp4 ")
                        || lowerOutput.contains(" m4a ")
                        || lowerOutput.contains("webm");

        boolean hasRobotError =
                lowerOutput.contains("robot")
                        || lowerOutput.contains("sign in to confirm")
                        || lowerOutput.contains("đăng nhập")
                        || lowerOutput.contains("dang nhap");

        boolean onlyStoryboard =
                lowerOutput.contains("only images are available")
                        || (
                        lowerOutput.contains("storyboard")
                                && !lowerOutput.contains(" mp4 ")
                                && !lowerOutput.contains(" m4a ")
                );

        return hasAvailableFormats
                && hasVideoOrAudioFormat
                && !hasRobotError
                && !onlyStoryboard;
    }

    private String explainYoutubeCookieError(String output) {
        if (output == null) {
            output = "";
        }

        String lowerOutput = output.toLowerCase();

        if (lowerOutput.contains("only images are available")
                || lowerOutput.contains("storyboard")) {
            return """
                    Nguyên nhân có thể:
                    - Cookies chưa hợp lệ hoặc đã hết hạn.
                    - YouTube vẫn chưa cho yt-dlp lấy format video thật.
                    - Deno/EJS chưa chạy đúng.

                    Cách xử lý:
                    - Export lại youtube_cookies.txt.
                    - Đảm bảo mở YouTube bằng trình duyệt vẫn xem video bình thường.
                    - Kiểm tra lại deno.exe trong tools/deno.
                    """;
        }

        if (lowerOutput.contains("robot")
                || lowerOutput.contains("sign in to confirm")
                || lowerOutput.contains("đăng nhập")
                || lowerOutput.contains("dang nhap")) {
            return """
                    Nguyên nhân:
                    - YouTube đang yêu cầu xác nhận không phải robot.
                    - Cookies hiện tại không đủ hoặc đã hết hạn.

                    Cách xử lý:
                    - Mở YouTube trên trình duyệt, đăng nhập và xác minh nếu cần.
                    - Export lại youtube_cookies.txt.
                    - Thay file cookies cũ trong data/cookies.
                    """;
        }

        if (lowerOutput.contains("could not open cookies")
                || lowerOutput.contains("no such file")
                || lowerOutput.contains("cookies")) {
            return """
                    Nguyên nhân:
                    - File cookies không đọc được, sai định dạng hoặc sai đường dẫn.

                    Cách xử lý:
                    - Kiểm tra file data/cookies/youtube_cookies.txt.
                    - Đảm bảo file không rỗng.
                    - File phải là định dạng Netscape cookies.txt, không phải JSON.
                    """;
        }

        if (lowerOutput.contains("deno")
                || lowerOutput.contains("javascript runtime")
                || lowerOutput.contains("js challenge")
                || lowerOutput.contains("n challenge")) {
            return """
                    Nguyên nhân:
                    - Deno hoặc EJS challenge solver chưa chạy đúng.

                    Cách xử lý:
                    - Kiểm tra tools/deno/deno.exe.
                    - Nếu chạy bản cài, đảm bảo deno.exe đã được copy theo installer.
                    """;
        }

        return """
                Chưa xác định rõ nguyên nhân.
                Hãy xem phần Output bên dưới để biết yt-dlp báo gì.
                """;
    }

    private String shortenOutput(String output, int maxLines) {
        if (output == null || output.isBlank()) {
            return "";
        }

        String[] lines = output.split("\\R");
        StringBuilder builder = new StringBuilder();

        int count = Math.min(lines.length, maxLines);

        for (int i = 0; i < count; i++) {
            builder.append(lines[i]).append(System.lineSeparator());
        }

        if (lines.length > maxLines) {
            builder.append("... còn ")
                    .append(lines.length - maxLines)
                    .append(" dòng nữa.");
        }

        return builder.toString();
    }

    private CommandResult runCommand(List<String> command, int timeoutSeconds) {
        Process process = null;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            processBuilder.directory(AppPaths.rootDir().toFile());

            addBundledDenoToPath(processBuilder);

            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            processBuilder.environment().put("PYTHONUTF8", "1");
            processBuilder.environment().put("LANG", "vi_VN.UTF-8");
            processBuilder.environment().put("LC_ALL", "vi_VN.UTF-8");

            process = processBuilder.start();

            StringBuilder outputBuilder = new StringBuilder();

            Process finalProcess = process;

            Thread outputReaderThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(finalProcess.getInputStream(), StandardCharsets.UTF_8)
                )) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append(System.lineSeparator());
                    }
                } catch (Exception exception) {
                    outputBuilder.append("Không đọc được output: ")
                            .append(exception.getMessage())
                            .append(System.lineSeparator());
                }
            });

            outputReaderThread.start();

            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeoutSeconds * 1000L;

            while (process.isAlive()) {
                long runningTime = System.currentTimeMillis() - startTime;

                if (runningTime > timeoutMillis) {
                    process.destroyForcibly();

                    return new CommandResult(
                            false,
                            -1,
                            "Lệnh kiểm tra cookies chạy quá "
                                    + timeoutSeconds
                                    + " giây nên tool đã tự dừng."
                    );
                }

                Thread.sleep(500);
            }

            int exitCode = process.exitValue();

            outputReaderThread.join(3000);

            return new CommandResult(
                    exitCode == 0,
                    exitCode,
                    outputBuilder.toString()
            );

        } catch (Exception exception) {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }

            return new CommandResult(
                    false,
                    -1,
                    exception.getMessage()
            );
        }
    }

    private void addBundledDenoToPath(ProcessBuilder processBuilder) {
        Path denoFile = AppPaths.denoFile();

        if (!Files.exists(denoFile)) {
            return;
        }

        Path denoFolder = denoFile.getParent();

        String currentPath = processBuilder.environment().getOrDefault("PATH", "");

        processBuilder.environment().put(
                "PATH",
                denoFolder.toAbsolutePath() + ";" + currentPath
        );
    }

    private record CommandResult(
            boolean success,
            int exitCode,
            String output
    ) {
    }
}
