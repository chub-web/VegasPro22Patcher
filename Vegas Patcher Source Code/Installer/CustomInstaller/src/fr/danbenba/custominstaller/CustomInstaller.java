package fr.danbenba.custominstaller;

import javax.swing.*;
import java.net.URISyntaxException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.CountDownLatch;
import java.util.*;
import java.util.zip.*;

// *** Zip4j ***
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

// *** CommonMark (Markdown -> HTML) ***
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

@SuppressWarnings({ "serial", "unused" })
public class CustomInstaller extends JFrame {

    private JTextField txtEmplacement;
    private JButton btnInstaller;
    private JButton btnClose;
    private JProgressBar progressBar;
    private JLabel lblStatus;
    private JLabel lblGitHubLink;
    private JLabel lblFooter;
    private JLabel lblImage;
    private JButton btnAdvancedOptions;
    private JCheckBox chkCreateShortcut;

    private static CountDownLatch latch = new CountDownLatch(1);

    private Properties config;
    private PrintWriter logWriter;

    public CustomInstaller() throws MalformedURLException {
        // 1) Charger la config
        config = loadConfig();

        // (Optionnel) Mot de passe avant lancement
        boolean launchPasswordEnabled = Boolean.parseBoolean(
                config.getProperty("launch.password.enabled", "false")
        );
        if (launchPasswordEnabled) {
            String correctPassword = config.getProperty("launch.password", "");
            while (true) {
                String enteredPassword = JOptionPane.showInputDialog(
                        null,
                        "Veuillez entrer le mot de passe pour lancer l'installateur :",
                        "Authentification requise",
                        JOptionPane.QUESTION_MESSAGE
                );
                if (enteredPassword == null) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Installation annulée.",
                            "Quitter",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    System.exit(0);
                }
                if (enteredPassword.equals(correctPassword)) {
                    break;
                } else {
                    int retry = JOptionPane.showConfirmDialog(
                            null,
                            "Mot de passe incorrect. Réessayer ?",
                            "Erreur",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (retry != JOptionPane.YES_OPTION) {
                        JOptionPane.showMessageDialog(
                                null,
                                "Installation annulée.",
                                "Quitter",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        System.exit(0);
                    }
                }
            }
        }

        // 2) Init log
        initLogFile();
        log("----- Lancement de l'installeur -----");

        // 3) Check Java
        if (!checkJavaVersion()) {
            System.exit(0);
        }

        // Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log("Impossible de configurer le LookAndFeel: " + e.getMessage());
        }

        setTitle(config.getProperty("app.title"));

        // *** 1) Permettre la fermeture de la fenêtre principale via la croix Windows ***
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setResizable(false);

        ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource(config.getProperty("app.iconPath"))));
        setIconImage(icon.getImage());
        setLayout(null);

        // Bannière
        ImageIcon originalIcon = new ImageIcon(new URL(config.getProperty("app.mainImage")));
        Image originalImage = originalIcon.getImage();
        int originalWidth = originalIcon.getIconWidth();
        int originalHeight = originalIcon.getIconHeight();
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth = 400;
        int newHeight = (int) (newWidth / aspectRatio);
        Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        final ImageIcon bannerIcon = new ImageIcon(scaledImage);

        lblImage = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int x = (getWidth() - bannerIcon.getIconWidth()) / 2;
                int y = (getHeight() - bannerIcon.getIconHeight()) / 2;
                g.drawImage(bannerIcon.getImage(), x, y, this);
            }
        };
        lblImage.setBounds(50, 10, 400, newHeight + 20);
        add(lblImage);

        // Champ chemin
        txtEmplacement = new JTextField(config.getProperty("app.dir"));
        txtEmplacement.setBounds(10, 230, 470, 25);
        add(txtEmplacement);

        // Label status
        lblStatus = new JLabel("Waiting for installation...");
        lblStatus.setBounds(10, 260, 470, 25);
        lblStatus.setForeground(Color.BLACK);
        add(lblStatus);

        // Progress Bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setBounds(10, 290, 470, 25);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        add(progressBar);

        // Bouton Installer
        btnInstaller = new JButton("Installer");
        btnInstaller.setBounds(10, 320, 100, 25);
        btnInstaller.addActionListener(e -> {
            boolean alreadyInstalledCheckEnabled = Boolean.parseBoolean(
                    config.getProperty("alreadyInstalledCheck.enabled", "true")
            );
            if (alreadyInstalledCheckEnabled) {
                if (isProgramAlreadyInstalled()) {
                    int choice = JOptionPane.showConfirmDialog(
                            CustomInstaller.this,
                            "Le programme semble déjà installé. Continuer et écraser l'installation existante ?",
                            "Programme déjà installé",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (choice != JOptionPane.YES_OPTION) {
                        log("Installation abandonnée car le programme est déjà installé.");
                        return;
                    }
                }
            }
            showMarkdownAndStartInstallation();
            btnInstaller.setEnabled(false);
            progressBar.setVisible(true);
            lblStatus.setText("Installing...");
        });
        add(btnInstaller);

        // Bouton Close
        btnClose = new JButton("Close");
        btnClose.setBounds(120, 320, 100, 25);
        btnClose.addActionListener(e -> {
            log("Fermeture de l'installeur (bouton Close).");
            dispose();
        });
        add(btnClose);

        // Case à cocher pour raccourci
        if ("true".equalsIgnoreCase(config.getProperty("shortcut.enabled", "false"))) {
            chkCreateShortcut = new JCheckBox("Créer un raccourci");
            chkCreateShortcut.setBounds(365, 260, 300, 25);
            add(chkCreateShortcut);
        }

        // Bouton options avancées
        if (Boolean.parseBoolean(config.getProperty("advanced.enabled", "true"))) {
            addAdvancedOptionsButton();
        }

        // (Lien GitHub) - Optionnel
        lblGitHubLink = new JLabel(config.getProperty("link.name", ""));
        lblGitHubLink.setBounds(375, 321, 230, 25);
        lblGitHubLink.setForeground(Color.BLUE);
        lblGitHubLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblGitHubLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(config.getProperty("link.url", "https://github.com/danbenba/CustomInstaller")));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });
        // add(lblGitHubLink); // si besoin

        // Footer (version)
        lblFooter = new JLabel("---- Data not found ----");
        lblFooter.setBounds(10, 200, 470, 25);
        lblFooter.setForeground(Color.BLACK);
        lblFooter.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblFooter);

        updateVersionLabel();
    }

    /**
     * Chargement config
     */
    private Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/resources/config/config.properties")) {
            if (input == null) {
                throw new FileNotFoundException("Configuration file 'config.properties' not found in the classpath.");
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }

    /**
     * Init log
     */
    private void initLogFile() {
        boolean loggingEnabled = Boolean.parseBoolean(config.getProperty("logging.enabled", "false"));
        if (!loggingEnabled) {
            return;
        }
        String logPath = config.getProperty("logging.path", System.getProperty("user.home") + "/installer_log.txt");
        logPath = logPath.replace("${user.home}", System.getProperty("user.home"));
        try {
            File logFile = new File(logPath);
            logWriter = new PrintWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void log(String message) {
        if (logWriter != null) {
            logWriter.println(message);
            logWriter.flush();
        }
    }

    /**
     * Vérification Java
     */
    private boolean checkJavaVersion() {
        int minVersion = Integer.parseInt(config.getProperty("java.minVersion", "11"));
        String version = System.getProperty("java.version");
        log("Java détecté : " + version + " (min requis : " + minVersion + ")");

        int majorVersion = 0;
        try {
            String[] parts = version.split("\\.");
            if (parts[0].equals("1")) {
                majorVersion = Integer.parseInt(parts[1]);
            } else {
                majorVersion = Integer.parseInt(parts[0]);
            }
        } catch (NumberFormatException e) {
            log("Impossible de parser la version Java: " + e.getMessage());
        }

        if (majorVersion < minVersion) {
            JOptionPane.showMessageDialog(
                    this,
                    "Java détecté : " + version
                            + ". Minimum requis : " + minVersion
                            + ". Veuillez mettre à jour votre JRE/JDK.",
                    "Java insuffisant",
                    JOptionPane.ERROR_MESSAGE
            );
            log("Java version insuffisante: " + version + " (min=" + minVersion + ")");
            return false;
        }
        return true;
    }

    /**
     * Détection si déjà installé
     */
    private boolean isProgramAlreadyInstalled() {
        Path installPath = Paths.get(txtEmplacement.getText());
        String alreadyInstalledFile = config.getProperty("alreadyInstalledCheck.file", "CustomInstaller.exe");
        Path mainExe = installPath.resolve(alreadyInstalledFile);
        return Files.exists(mainExe);
    }

    /**
     * Bouton options avancées
     */
    private void addAdvancedOptionsButton() {
        btnAdvancedOptions = new JButton("Options avancées");
        btnAdvancedOptions.setBounds(230, 320, 130, 25);
        btnAdvancedOptions.addActionListener(e -> {
            JDialog advancedDialog = new JDialog(this, "Paramètres avancés", true);
            advancedDialog.setSize(400, 300);
            advancedDialog.setLayout(null);

            JLabel lblProxy = new JLabel("Proxy : ");
            lblProxy.setBounds(10, 10, 100, 25);
            advancedDialog.add(lblProxy);

            JTextField txtProxy = new JTextField();
            txtProxy.setBounds(120, 10, 200, 25);
            advancedDialog.add(txtProxy);

            JButton btnOK = new JButton("OK");
            btnOK.setBounds(150, 220, 80, 25);
            btnOK.addActionListener(ev -> {
                advancedDialog.dispose();
            });
            advancedDialog.add(btnOK);

            advancedDialog.setLocationRelativeTo(this);
            advancedDialog.setVisible(true);
        });
        add(btnAdvancedOptions);
    }

    private void updateVersionLabel() {
        try {
            URL url = new URL(config.getProperty("version.url"));
            String versionText = downloadText(url);
            lblFooter.setText("---- " + versionText + " ----");
        } catch (IOException e) {
            lblFooter.setText("Server Closed");
            log("Erreur lors de la récupération de la version: " + e.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    "Internal Error: Server Closed.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String downloadText(URL url) throws IOException {
        StringBuilder response = new StringBuilder();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
        }
        return response.toString();
    }

    /**
     * 2) Afficher CGU + lancer installation
     */
    private void showMarkdownAndStartInstallation() {
        JDialog dialog = new JDialog(this, "Terms and Conditions", true);
        dialog.setSize(800, 630);
        dialog.setResizable(false);
        dialog.setLayout(null);

        // *** Empêcher la fermeture silencieuse => On annule l'installation si on ferme la fenêtre ***
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                log("Installation annulée depuis l'écran des CGU (croix).");
                dialog.dispose();
                // Réactiver le bouton "Installer" + cacher la barre + message "Installation canceled"
                btnInstaller.setEnabled(true);
                progressBar.setVisible(false);
                lblStatus.setText("Installation canceled");
            }
        });

        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);

        try {
            // On télécharge le Markdown depuis l’URL
            URL url = new URL(config.getProperty("terms.url"));
            String termsContent = downloadText(url);

            // 3) Forcer la police Windows 11 (Segoe UI) + converter Markdown -> HTML
            String style = "<style>body { font-family: 'Segoe UI'; font-size: 14px; }</style>";
            String mdHtml = convertMarkdownToHtml(termsContent);
            String htmlContent = "<html><head>" + style + "</head><body>" + mdHtml + "</body></html>";

            textPane.setText(htmlContent);
            textPane.setCaretPosition(0); // Remonter en haut

        } catch (IOException e) {
            textPane.setText("<html><body><h3>Failed to load content.</h3></body></html>");
        }

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBounds(10, 10, 760, 500);
        dialog.add(scrollPane);

        JCheckBox chkAccept = new JCheckBox("I Accept the Terms and Conditions");
        chkAccept.setBounds(10, 520, 300, 25);
        dialog.add(chkAccept);

        JButton btnDownload = new JButton("Install");
        JButton btnExit = new JButton("Exit");

        int buttonWidth = 120;
        int buttonHeight = 25;
        int buttonYPosition = 550;
        int dialogWidth = dialog.getWidth();
        int totalButtonWidth = buttonWidth * 2 + 20;
        int startXPosition = (dialogWidth - totalButtonWidth) / 2;

        btnDownload.setBounds(startXPosition, buttonYPosition, buttonWidth, buttonHeight);
        btnDownload.setEnabled(false);
        btnDownload.addActionListener(ae -> {
            dialog.dispose();
            installer();
        });
        dialog.add(btnDownload);

        btnExit.setBounds(startXPosition + buttonWidth + 20, buttonYPosition, buttonWidth, buttonHeight);
        btnExit.addActionListener(ae -> {
            log("Installation annulée depuis l'écran des CGU (bouton Exit).");
            dialog.dispose();
            btnInstaller.setEnabled(true);
            progressBar.setVisible(false);
            lblStatus.setText("Installation canceled");
        });
        dialog.add(btnExit);

        chkAccept.addActionListener(ae -> {
            btnDownload.setEnabled(chkAccept.isSelected());
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Convertir Markdown -> HTML avec CommonMark
     */
    private String convertMarkdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }

    /**
     * Méthode principale d'installation
     */
    private void installer() {
        progressBar.setVisible(true);

        // Vérif internet
        boolean netCheck = Boolean.parseBoolean(config.getProperty("internetCheck.enabled", "true"));
        if (netCheck && !checkInternetConnectivity()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Impossible de se connecter à Internet. Vérifiez votre connexion.",
                    "Pas de connexion",
                    JOptionPane.ERROR_MESSAGE
            );
            log("Installation annulée : pas de connexion Internet.");
            lblStatus.setText("Installation canceled");
            return;
        }

        // Vérif espace disque
        if (!checkDiskSpace(Paths.get(txtEmplacement.getText()))) {
            log("Installation annulée : espace disque insuffisant.");
            return;
        }

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                try {
                    lblStatus.setText("Downloading files...");
                    log("Début du téléchargement...");
                    progressBar.setValue(0);

                    URL url = new URL(config.getProperty("download.url"));
                    Path tempDir = Files.createTempDirectory("CustomInstaller");
                    boolean resumable = Boolean.parseBoolean(config.getProperty("download.resumable", "false"));
                    File zipFile;
                    if (resumable) {
                        zipFile = downloadFileResumable(url, tempDir);
                    } else {
                        zipFile = downloadFile(url, tempDir);
                    }

                    lblStatus.setText("Unpacking " + zipFile.getName() + "...");
                    progressBar.setValue(30);
                    Thread.sleep(200);

                    boolean zipPasswordEnabled = Boolean.parseBoolean(config.getProperty("zip.password.enabled", "false"));
                    String zipPassword = config.getProperty("zip.password", "");

                    if (zipPasswordEnabled) {
                        unzipWithPassword(zipFile, tempDir, zipPassword);
                    } else {
                        unzip(zipFile, tempDir);
                    }
                    zipFile.delete();

                    Path targetDir = Paths.get(txtEmplacement.getText());
                    Files.walk(tempDir).forEach(source -> {
                        try {
                            if (!Files.isDirectory(source)) {
                                lblStatus.setText("Copying file " + source.getFileName());
                                log("Copie du fichier: " + source);
                                copy(source, targetDir.resolve(tempDir.relativize(source)));
                            }
                        } catch (Exception ex) {
                            log("Erreur de copie: " + ex.getMessage());
                        }
                    });

                    boolean uninstallerEnabled = Boolean.parseBoolean(config.getProperty("uninstaller.enabled", "true"));
                    if (uninstallerEnabled) {
                        createUninstaller(targetDir);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    log("Erreur dans l'installation: " + ex.getMessage());
                    lblStatus.setText("Error: " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                progressBar.setValue(100);
                lblStatus.setText("Installation finished");
                log("Installation terminée.");

                if (chkCreateShortcut != null && chkCreateShortcut.isSelected()) {
                    try {
                        createShortcuts();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        lblStatus.setText("Impossible de créer les raccourcis !");
                        log("Echec de la création de raccourcis: " + ex.getMessage());
                    }
                }

                boolean launchPrompt = Boolean.parseBoolean(config.getProperty("launch.prompt", "true"));
                if (launchPrompt) {
                    proposeToRunApplication();
                }
            }
        };
        worker.execute();
    }

    /**
     * Vérif internet
     */
    private boolean checkInternetConnectivity() {
        try {
            URL testUrl = new URL("https://www.google.com");
            HttpURLConnection connection = (HttpURLConnection) testUrl.openConnection();
            connection.setConnectTimeout(5000);
            connection.connect();
            return connection.getResponseCode() == 200;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Vérif espace disque
     */
    private boolean checkDiskSpace(Path installPath) {
        long requiredBytes = Long.parseLong(config.getProperty("installer.minSpace", "200000000"));
        try {
            if (Files.notExists(installPath)) {
                int choice = JOptionPane.showConfirmDialog(
                        this,
                        "Le dossier " + installPath + " n'existe pas.\nVoulez-vous le créer ?",
                        "Dossier inexistant",
                        JOptionPane.YES_NO_OPTION
                );
                if (choice == JOptionPane.YES_OPTION) {
                    Files.createDirectories(installPath);
                    log("Dossier créé : " + installPath);
                } else {
                    log("Utilisateur a refusé de créer le dossier : " + installPath);
                    lblStatus.setText("Installation canceled");
                    return false;
                }
            }

            FileStore store = Files.getFileStore(installPath);
            long available = store.getUsableSpace();
            if (available < requiredBytes) {
                JOptionPane.showMessageDialog(
                        this,
                        "Espace disque insuffisant !\n"
                                + "Requis : " + requiredBytes + " octets\n"
                                + "Disponible : " + available + " octets",
                        "Erreur d'espace disque",
                        JOptionPane.ERROR_MESSAGE
                );
                log("Espace disque insuffisant. Requis: " + requiredBytes + " Dispo: " + available);
                lblStatus.setText("Installation canceled");
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            log("Erreur lors de la vérification d'espace disque: " + e.getMessage());
            lblStatus.setText("Installation canceled (Erreur)");
            return false;
        }
        return true;
    }

    /**
     * Téléchargement simple
     */
    private File downloadFile(URL url, Path tempDir) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        long contentLength = connection.getContentLengthLong();
        File file = tempDir.resolve("downloaded.zip").toFile();

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            long totalBytesRead = 0;
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                if (contentLength > 0) {
                    int progress = (int)((totalBytesRead * 100) / contentLength);
                    progressBar.setValue(progress);
                    lblStatus.setText("Téléchargement en cours: " + progress + "%");
                }
            }
        }
        return file;
    }

    /**
     * Téléchargement résumable
     */
    private File downloadFileResumable(URL url, Path tempDir) throws IOException {
        File partialFile = tempDir.resolve("downloaded.zip").toFile();
        long existingSize = partialFile.exists() ? partialFile.length() : 0;

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (existingSize > 0) {
            connection.setRequestProperty("Range", "bytes=" + existingSize + "-");
            log("Reprise du téléchargement à partir de " + existingSize + " octets.");
        }
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
            log("Serveur ne supporte pas la reprise (code=" + responseCode + "), on retente en normal.");
            return downloadFile(url, tempDir);
        }

        long contentLength = connection.getContentLengthLong() + existingSize;

        try (RandomAccessFile raf = new RandomAccessFile(partialFile, "rw");
             InputStream in = connection.getInputStream()) {

            raf.seek(existingSize);
            byte[] buffer = new byte[4096];
            long totalBytesRead = existingSize;
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                raf.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                if (contentLength > 0) {
                    int progress = (int)((totalBytesRead * 100) / contentLength);
                    progressBar.setValue(progress);
                    lblStatus.setText("Downloading " + progress + "%");
                }
            }
        }
        return partialFile;
    }

    /**
     * Dézipper normal
     */
    private void unzip(File zipFile, Path outputPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Path newPath = zipSlipProtect(zipEntry, outputPath);
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null && Files.notExists(newPath.getParent())) {
                        Files.createDirectories(newPath.getParent());
                    }
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zis.getNextEntry();
            }
        }
        log("Fichiers dézippés dans: " + outputPath);
    }

    /**
     * Dézipper avec mot de passe (Zip4j)
     */
    private void unzipWithPassword(File zipFile, Path outputPath, String password) throws IOException {
        try {
            ZipFile zf = new ZipFile(zipFile);
            if (zf.isEncrypted()) {
                zf.setPassword(password.toCharArray());
            }
            zf.extractAll(outputPath.toString());
            log("Fichiers dézippés (avec mot de passe) dans: " + outputPath);
        } catch (ZipException e) {
            log("Erreur Zip4j: " + e.getMessage());
            throw new IOException("Impossible de dézipper l'archive protégée.");
        }
    }

    private Path zipSlipProtect(ZipEntry zipEntry, Path outputPath) throws IOException {
        Path targetDirResolved = outputPath.resolve(zipEntry.getName());
        Path normalizedPath = targetDirResolved.normalize();
        if (!normalizedPath.startsWith(outputPath)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
        }
        return normalizedPath;
    }

    private void copy(Path source, Path dest) {
        try {
            if (!Files.isDirectory(source)) {
                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            lblStatus.setText("Error copying files: " + e.getMessage());
            log("Erreur de copie: " + e.getMessage());
        }
    }

    /**
     * Créer un désinstalleur
     */
    private void createUninstaller(Path installDir) throws IOException {
        Path uninstaller = installDir.resolve("uninstall.bat");
        try (BufferedWriter writer = Files.newBufferedWriter(uninstaller, StandardOpenOption.CREATE)) {
            writer.write("@echo off");
            writer.newLine();
            writer.write("echo Suppression des fichiers...");
            writer.newLine();
            writer.write("del /q \"" + installDir.toString() + "\\*.*\"");
            writer.newLine();
            writer.write("rmdir /s /q \"" + installDir.toString() + "\"");
            writer.newLine();
            writer.write("echo Desinstallation terminee.");
            writer.newLine();
            writer.write("pause");
        }
        log("Uninstaller créé: " + uninstaller);
    }

    /**
     * Créer les raccourcis
     */
    private void createShortcuts() throws IOException {
        String shortcutName = config.getProperty("shortcut.name", "Custom Installer");
        String targetFile   = config.getProperty("shortcut.target", "CustomInstaller.exe");

        Path installPath = Paths.get(txtEmplacement.getText());
        Path targetFullPath = installPath.resolve(targetFile);

        String userHome = System.getProperty("user.home");
        Path desktopPath = Paths.get(userHome, "Desktop");
        Path startMenuPath = Paths.get(userHome, "AppData", "Roaming", "Microsoft", "Windows", "Start Menu", "Programs");

        Path desktopShortcut = desktopPath.resolve(shortcutName + ".lnk");
        Path startMenuShortcut = startMenuPath.resolve(shortcutName + ".lnk");

        createWindowsShortcut(targetFullPath.toString(), desktopShortcut.toString());
        createWindowsShortcut(targetFullPath.toString(), startMenuShortcut.toString());
        log("Raccourcis créés sur le bureau et dans le Menu Démarrer.");
    }

    private void createWindowsShortcut(String target, String shortcutPath) throws IOException {
        String vbsScript =
                "Set shell = CreateObject(\"WScript.Shell\")\n" +
                        "Set shortcut = shell.CreateShortcut(\"" + shortcutPath + "\")\n" +
                        "shortcut.TargetPath = \"" + target + "\"\n" +
                        "shortcut.WorkingDirectory = \"" + new File(target).getParent() + "\"\n" +
                        "shortcut.WindowStyle = 1\n" +
                        "shortcut.IconLocation = \"" + target + ",0\"\n" +
                        "shortcut.Save\n";

        File tempVbs = File.createTempFile("shortcut", ".vbs");
        try (FileWriter fw = new FileWriter(tempVbs)) {
            fw.write(vbsScript);
        }

        Process p = Runtime.getRuntime().exec("wscript \"" + tempVbs.getAbsolutePath() + "\"");
        try {
            p.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            tempVbs.delete();
        }
    }

    /**
     * Proposer de lancer l'app
     */
    private void proposeToRunApplication() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Installation des fichiers terminée. Souhaitez-vous lancer le patch  ?",
                "Installation des fichiers terminée",
                JOptionPane.YES_NO_OPTION
        );
        if (choice == JOptionPane.YES_OPTION) {
            try {
                Path exePath = Paths.get(
                        txtEmplacement.getText(),
                        config.getProperty("shortcut.target", "CustomInstaller.exe")
                );
                Desktop.getDesktop().open(exePath.toFile());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        this,
                        "Impossible de lancer l'application.",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /**
     * isRunAsAdmin (exemple Windows)
     */
    private boolean isRunAsAdmin() {
        File testPrivilege = new File("C:\\testPrivilege.txt");
        try {
            if (testPrivilege.createNewFile()) {
                testPrivilege.delete();
                return true;
            }
        } catch (IOException e) {
            // rien
        }
        return false;
    }

    // Main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new CustomInstaller().setVisible(true);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
