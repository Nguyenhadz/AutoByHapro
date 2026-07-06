package com.hapro.autobyhapro.license.admin;

import com.hapro.autobyhapro.license.LicenseConstants;
import com.hapro.autobyhapro.license.LicenseCryptoUtil;
import com.hapro.autobyhapro.license.LicenseInfo;
import com.hapro.autobyhapro.license.LicenseType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;

public class LicenseAdminGui extends JFrame {

    private final JTextField machineIdField = new JTextField();
    private final JTextField customerNameField = new JTextField();
    private final JComboBox<LicenseTypeOption> licenseTypeComboBox = new JComboBox<>();
    private final JTextField outputFolderField = new JTextField();
    private final JTextArea logArea = new JTextArea();

    private Path appRoot;
    private Path privateKeyFile;
    private Path publicKeyBase64File;
    private Path outputFolder;

    public LicenseAdminGui() {
        super("Hapro License Admin");

        detectPaths();
        buildLayout();
        refreshStatus();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            LicenseAdminGui frame = new LicenseAdminGui();
            frame.setVisible(true);
        });
    }

    private void detectPaths() {
        appRoot = findAppRoot();

        privateKeyFile = appRoot
                .resolve("license_admin")
                .resolve("private_key.pem")
                .toAbsolutePath()
                .normalize();

        publicKeyBase64File = appRoot
                .resolve("license_admin")
                .resolve("public_key_base64.txt")
                .toAbsolutePath()
                .normalize();

        outputFolder = appRoot
                .resolve("generated_licenses")
                .toAbsolutePath()
                .normalize();
    }

    private Path findAppRoot() {
        Path currentDir = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize();

        if (Files.exists(currentDir.resolve("license_admin").resolve("private_key.pem"))) {
            return currentDir;
        }

        try {
            Path codeLocation = Path.of(
                    LicenseAdminGui.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).toAbsolutePath().normalize();

            Path checkPath = Files.isRegularFile(codeLocation)
                    ? codeLocation.getParent()
                    : codeLocation;

            while (checkPath != null) {
                if (Files.exists(checkPath.resolve("license_admin").resolve("private_key.pem"))) {
                    return checkPath;
                }

                if (Files.exists(checkPath.resolve("app").resolve("license_admin").resolve("private_key.pem"))) {
                    return checkPath.resolve("app");
                }

                checkPath = checkPath.getParent();
            }

        } catch (Exception ignored) {
        }

        return currentDir;
    }

    private void buildLayout() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 680);
        setLocationRelativeTo(null);

        JPanel rootPanel = new JPanel(new BorderLayout(12, 12));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel titleLabel = new JLabel("Hapro License Admin");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));

        JLabel subtitleLabel = new JLabel("Tạo licence 30 ngày / 1 năm / vĩnh viễn cho từng máy.");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel headerPanel = new JPanel(new BorderLayout(4, 4));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.CENTER);

        JPanel formPanel = buildFormPanel();
        JPanel buttonPanel = buildButtonPanel();

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));

        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log"));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(buttonPanel, BorderLayout.CENTER);

        rootPanel.add(headerPanel, BorderLayout.NORTH);
        rootPanel.add(centerPanel, BorderLayout.CENTER);
        rootPanel.add(logScrollPane, BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Thông tin licence"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        machineIdField.setFont(new Font("Consolas", Font.PLAIN, 14));
        customerNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        outputFolderField.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputFolderField.setEditable(false);

        licenseTypeComboBox.addItem(new LicenseTypeOption(LicenseType.MONTH_1));
        licenseTypeComboBox.addItem(new LicenseTypeOption(LicenseType.YEAR_1));
        licenseTypeComboBox.addItem(new LicenseTypeOption(LicenseType.LIFETIME));

        outputFolderField.setText(outputFolder.toString());

        addRow(panel, gbc, 0, "Mã máy khách gửi:", machineIdField);
        addRow(panel, gbc, 1, "Tên khách hàng:", customerNameField);
        addRow(panel, gbc, 2, "Loại licence:", licenseTypeComboBox);
        addRow(panel, gbc, 3, "Folder xuất licence:", outputFolderField);

        return panel;
    }

    private void addRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String labelText,
            java.awt.Component component
    ) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.gridwidth = 2;

        panel.add(component, gbc);
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Thao tác"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        JButton generateButton = new JButton("Tạo licence");
        generateButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        generateButton.addActionListener(event -> generateLicense());

        JButton chooseFolderButton = new JButton("Chọn folder xuất");
        chooseFolderButton.addActionListener(event -> chooseOutputFolder());

        JButton openOutputFolderButton = new JButton("Mở folder licence đã tạo");
        openOutputFolderButton.addActionListener(event -> openFolder(outputFolder));

        JButton openKeyFolderButton = new JButton("Mở folder key admin");
        openKeyFolderButton.addActionListener(event -> openFolder(appRoot.resolve("license_admin")));

        JButton copyPublicKeyButton = new JButton("Copy public key");
        copyPublicKeyButton.addActionListener(event -> copyPublicKey());

        JButton refreshButton = new JButton("Refresh trạng thái");
        refreshButton.addActionListener(event -> refreshStatus());

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(generateButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(chooseFolderButton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        panel.add(openOutputFolderButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(openKeyFolderButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(copyPublicKeyButton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        panel.add(refreshButton, gbc);

        return panel;
    }

    private void refreshStatus() {
        StringBuilder builder = new StringBuilder();

        builder.append("APP ROOT")
                .append(System.lineSeparator())
                .append(appRoot.toAbsolutePath())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("PRIVATE KEY")
                .append(System.lineSeparator())
                .append(privateKeyFile)
                .append(System.lineSeparator())
                .append("Tồn tại: ")
                .append(Files.exists(privateKeyFile) ? "Có" : "Không")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("PUBLIC KEY")
                .append(System.lineSeparator())
                .append(publicKeyBase64File)
                .append(System.lineSeparator())
                .append("Tồn tại: ")
                .append(Files.exists(publicKeyBase64File) ? "Có" : "Không")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("OUTPUT FOLDER")
                .append(System.lineSeparator())
                .append(outputFolder)
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("GHI CHÚ")
                .append(System.lineSeparator())
                .append("- Private key chỉ để trên máy admin của m.")
                .append(System.lineSeparator())
                .append("- Không copy folder license_admin cho khách.")
                .append(System.lineSeparator())
                .append("- File .lic tạo ra mới gửi cho khách import vào tool.");

        logArea.setText(builder.toString());
    }

    private void generateLicense() {
        try {
            String machineId = machineIdField.getText().trim();
            String customerName = customerNameField.getText().trim();

            if (machineId.isBlank()) {
                showWarning("Mã máy đang trống.");
                return;
            }

            if (customerName.isBlank()) {
                showWarning("Tên khách hàng đang trống.");
                return;
            }

            if (!Files.exists(privateKeyFile)) {
                showWarning("Không tìm thấy private_key.pem.\n\nCần có file:\n" + privateKeyFile);
                return;
            }

            Files.createDirectories(outputFolder);

            LicenseTypeOption selectedOption =
                    (LicenseTypeOption) licenseTypeComboBox.getSelectedItem();

            if (selectedOption == null) {
                showWarning("Chưa chọn loại licence.");
                return;
            }

            LicenseType licenseType = selectedOption.getLicenseType();

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

            String privateKeyBase64 =
                    LicenseCryptoUtil.readKeyBase64FromFile(privateKeyFile);

            String signature =
                    LicenseCryptoUtil.signLicenseInfo(unsignedInfo, privateKeyBase64);

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
            String safeCustomer = customerName.replaceAll("[^A-Za-z0-9_-]", "_");

            if (safeCustomer.isBlank()) {
                safeCustomer = "customer";
            }

            String fileName = "license_"
                    + safeCustomer
                    + "_"
                    + safeMachine
                    + "_"
                    + licenseType.name()
                    + ".lic";

            Path outputFile = outputFolder.resolve(fileName);

            if (Files.exists(outputFile)) {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "File licence đã tồn tại, ghi đè không?\n\n" + outputFile,
                        "Xác nhận ghi đè",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            Files.writeString(
                    outputFile,
                    LicenseCryptoUtil.toJson(signedInfo),
                    StandardCharsets.UTF_8
            );

            logArea.setText(
                    "ĐÃ TẠO LICENCE THÀNH CÔNG"
                            + System.lineSeparator()
                            + "==========================="
                            + System.lineSeparator()
                            + "File: " + outputFile.toAbsolutePath()
                            + System.lineSeparator()
                            + "Khách hàng: " + customerName
                            + System.lineSeparator()
                            + "Mã máy: " + machineId
                            + System.lineSeparator()
                            + "Loại licence: " + licenseType.getDisplayName()
                            + System.lineSeparator()
                            + "Ngày cấp: " + issuedAt
                            + System.lineSeparator()
                            + "Ngày hết hạn: " + (licenseType.isLifetime() ? "Vĩnh viễn" : expiresAt)
                            + System.lineSeparator()
                            + System.lineSeparator()
                            + "Gửi file .lic này cho khách để import vào tool."
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Đã tạo licence thành công.\n\n" + outputFile.toAbsolutePath(),
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception exception) {
            logArea.setText("Tạo licence thất bại:\n" + exception.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    exception.getMessage(),
                    "Lỗi tạo licence",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void chooseOutputFolder() {
        JFileChooser chooser = new JFileChooser(outputFolder.toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Chọn folder xuất licence");

        int result = chooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFolder = chooser.getSelectedFile();

        if (selectedFolder == null) {
            return;
        }

        outputFolder = selectedFolder.toPath().toAbsolutePath().normalize();
        outputFolderField.setText(outputFolder.toString());

        refreshStatus();
    }

    private void copyPublicKey() {
        try {
            if (!Files.exists(publicKeyBase64File)) {
                showWarning("Không tìm thấy public_key_base64.txt:\n" + publicKeyBase64File);
                return;
            }

            String text = Files.readString(publicKeyBase64File, StandardCharsets.UTF_8).trim();

            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(text), null);

            logArea.setText(
                    "Đã copy public key vào clipboard."
                            + System.lineSeparator()
                            + System.lineSeparator()
                            + "Public key này dùng để dán vào LicenseConstants.PUBLIC_KEY_BASE64 trong app chính."
            );

        } catch (Exception exception) {
            showWarning("Không thể copy public key:\n" + exception.getMessage());
        }
    }

    private void openFolder(Path folder) {
        try {
            Files.createDirectories(folder);

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder.toFile());
            } else {
                new ProcessBuilder(
                        "explorer.exe",
                        folder.toAbsolutePath().toString()
                ).start();
            }

        } catch (Exception exception) {
            showWarning("Không thể mở folder:\n" + exception.getMessage());
        }
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Thông báo",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private static class LicenseTypeOption {

        private final LicenseType licenseType;

        private LicenseTypeOption(LicenseType licenseType) {
            this.licenseType = licenseType;
        }

        public LicenseType getLicenseType() {
            return licenseType;
        }

        @Override
        public String toString() {
            return licenseType.getDisplayName() + " - " + licenseType.name();
        }
    }
}