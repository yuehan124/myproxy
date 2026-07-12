package com.myproxy.update;

import com.myproxy.config.ConfigManager;
import com.myproxy.config.ProxyConfig;
import com.myproxy.ui.I18nManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for checking and applying automatic updates.
 *
 * <p>The update server (nginx) serves a plain-text version list file at
 * {@code {updateUrl}/versions.txt}, containing one version per line in
 * descending order (highest version first). The jar for each version is
 * available at {@code {updateUrl}/myproxy-{version}.jar}.</p>
 *
 * <p>On startup, if {@code autoUpdate} is enabled, the service fetches the
 * version list, compares the highest version with the local version, and
 * if a newer version exists, prompts the user to update. On confirmation,
 * the new jar is downloaded to a temporary file, then swaps with the
 * running jar and restarts the application.</p>
 *
 * @author yuehan124@gmail.com
 * @since 2026-07-12
 */
public class UpdateService {

    private static final Logger logger = LoggerFactory.getLogger(UpdateService.class);

    static {
        // Disable hostname verification for the built-in HttpClient,
        // so that HTTPS connections to IP addresses or self-signed certs work.
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
    }

    /** Current application version. */
    public static final String CURRENT_VERSION = detectVersion();

    /**
     * Detect the current application version. When packaged with jpackage,
     * the version is available via the {@code jpackage.app-version} system property.
     * Falls back to the hardcoded version for development (java -jar) mode.
     */
    private static String detectVersion() {
        String version = System.getProperty("jpackage.app-version");
        if (version != null && !version.isBlank()) {
            return version;
        }
        return "1.0.0";
    }

    private final ConfigManager configManager;
    private final I18nManager i18n;
    private final Runnable shutdownCallback;

    /**
     * @param configManager   config manager
     * @param shutdownCallback called to gracefully stop services and exit
     *                        (e.g. {@code mainFrame::exitApp})
     */
    public UpdateService(ConfigManager configManager, Runnable shutdownCallback) {
        this.configManager = configManager;
        this.i18n = I18nManager.getInstance();
        this.shutdownCallback = shutdownCallback;
    }

    /**
     * Asynchronously check for updates. If a newer version is found, prompt
     * the user (on the EDT) to confirm the update.
     */
    public void checkForUpdatesAsync() {
        ProxyConfig config = configManager.getConfig();
        if (!config.isAutoUpdate()) {
            return;
        }
        String updateUrl = config.getUpdateUrl();
        if (updateUrl == null || updateUrl.isBlank()) {
            return;
        }
        // Normalize: remove trailing slash
        if (updateUrl.endsWith("/")) {
            updateUrl = updateUrl.substring(0, updateUrl.length() - 1);
        }
        final String baseUrl = updateUrl;

        new Thread(() -> {
            try {
                logger.info("Checking for updates at {}", baseUrl);
                List<String> versions = fetchVersionList(baseUrl);
                if (versions.isEmpty()) {
                    logger.info("No versions found on update server");
                    return;
                }
                String latest = versions.get(0);
                logger.info("Latest version: {}, current: {}", latest, CURRENT_VERSION);
                if (compareVersions(latest, CURRENT_VERSION) <= 0) {
                    logger.info("Application is up to date");
                    return;
                }
                // New version available, prompt user on EDT
                SwingUtilities.invokeLater(() -> {
                    String message = i18n.getString("update.available", latest);
                    int choice = JOptionPane.showConfirmDialog(null,
                            message,
                            i18n.getString("update.title"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (choice == JOptionPane.YES_OPTION) {
                        performUpdate(baseUrl, latest);
                    }
                });
            } catch (Exception e) {
                logger.warn("Update check failed: {}", e.getMessage());
            }
        }, "update-checker").start();
    }

    /**
     * Fetch and parse the version list from the update server.
     *
     * @param baseUrl base URL of the update server
     * @return list of version strings (highest first)
     */
    private List<String> fetchVersionList(String baseUrl) throws IOException, InterruptedException {
        String url = baseUrl + "/versions.txt";
        HttpClient client = HttpClientFactory.createTrustAllClient(10);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.warn("Version list HTTP status: {}", response.statusCode());
            return List.of();
        }
        List<String> versions = new ArrayList<>();
        String body = response.body();
        if (body != null) {
            for (String line : body.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    versions.add(trimmed);
                }
            }
        }
        return versions;
    }

    /**
     * Download the new jar and apply the update.
     * <p>
     * The new jar is downloaded directly into the app directory (user-writable
     * with per-user install). The launcher config is updated to point to the
     * new jar. Old jars are kept for rollback. The application is then
     * restarted via the native launcher.
     *
     * @param baseUrl base URL of the update server
     * @param version target version to download
     */
    private void performUpdate(String baseUrl, String version) {
        new Thread(() -> {
            String downloadUrl = baseUrl + "/myproxy-" + version + ".jar";
            logger.info("Downloading update from {}", downloadUrl);
            try {
                // Determine the path of the currently running jar
                Path currentJar = getCurrentJarPath();
                if (currentJar == null) {
                    throw new IOException("Cannot determine current jar path");
                }
                Path appDir = currentJar.getParent();

                // Download the new jar directly into the app directory
                Path newJar = appDir.resolve("myproxy-" + version + ".jar");
                downloadJarTo(downloadUrl, newJar);
                logger.info("Update downloaded to {}", newJar);

                // Update MyProxy.cfg to point to the new jar (jpackage launcher config)
                updateLauncherConfig(appDir, newJar.getFileName().toString());

                // Prompt user to restart
                SwingUtilities.invokeLater(() -> {
                    int choice = JOptionPane.showConfirmDialog(null,
                            i18n.getString("update.confirm.restart"),
                            i18n.getString("update.title"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (choice == JOptionPane.YES_OPTION) {
                        restartApplication(newJar);
                    }
                });
            } catch (Exception e) {
                logger.error("Update failed", e);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(null,
                                i18n.getString("update.download.failed", e.getMessage()),
                                i18n.getString("update.title"),
                                JOptionPane.ERROR_MESSAGE));
            }
        }, "update-downloader").start();
    }

    /**
     * Update the jpackage launcher config ({@code MyProxy.cfg}) to point to
     * the new jar file.
     *
     * @param appDir   the {@code app/} directory containing the cfg file
     * @param jarName  the new jar file name (e.g. {@code myproxy-1.0.1.jar})
     */
    private void updateLauncherConfig(Path appDir, String jarName) throws IOException {
        Path cfgPath = findLauncherConfig(appDir);
        if (cfgPath == null) {
            logger.info("No launcher config found, skipping cfg update");
            return;
        }
        String content = Files.readString(cfgPath, java.nio.charset.StandardCharsets.UTF_8);
        // Replace app.classpath line: $APPDIR\myproxy-x.y.z.jar -> $APPDIR\myproxy-newVersion.jar
        // Use Matcher.quoteReplacement to avoid $ being interpreted as a group reference
        content = content.replaceAll(
                "app\\.classpath=.*",
                java.util.regex.Matcher.quoteReplacement("app.classpath=$APPDIR\\" + jarName));
        Files.writeString(cfgPath, content, java.nio.charset.StandardCharsets.UTF_8);
        logger.info("Updated launcher config: {} -> {}", cfgPath, jarName);
    }

    /**
     * Find the jpackage launcher config file ({@code MyProxy.cfg}).
     * It can be in the {@code app/} directory or its parent directory.
     *
     * @param appDir the {@code app/} directory
     * @return path to the cfg file, or {@code null} if not found
     */
    private Path findLauncherConfig(Path appDir) {
        // jpackage puts .cfg in appDir with the same name as the exe
        // Try common names: MyProxy.cfg, myproxy.cfg
        String[] candidates = {"MyProxy.cfg", "myproxy.cfg"};
        for (String name : candidates) {
            Path cfg = appDir.resolve(name);
            if (Files.exists(cfg)) {
                return cfg;
            }
        }
        return null;
    }

    /**
     * Download a jar file from the given URL to the specified path.
     *
     * @param url     the download URL
     * @param destPath destination file path
     */
    private void downloadJarTo(String url, Path destPath) throws IOException, InterruptedException {
        HttpClient client = HttpClientFactory.createTrustAllClient(10);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();
        try (InputStream is = client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body()) {
            Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Get the path of the currently running jar file.
     *
     * @return path to the jar, or {@code null} if not running from a jar
     */
    private Path getCurrentJarPath() {
        try {
            String path = UpdateService.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            File file = new File(path);
            if (file.isFile() && file.getName().endsWith(".jar")) {
                return file.toPath();
            }
        } catch (Exception e) {
            logger.warn("Failed to determine current jar path", e);
        }
        return null;
    }

    /**
     * Restart the application by relaunching the native launcher
     * (e.g. {@code MyProxy.exe}), then gracefully shutting down the current
     * process. Old jars are kept for rollback.
     *
     * @param newJar path to the new jar (used to locate the app directory)
     */
    private void restartApplication(Path newJar) {
        try {
            Path appDir = newJar.getParent();
            Path installDir = appDir.getParent();
            String launcherName = System.getProperty("os.name", "").toLowerCase().contains("win")
                    ? "MyProxy.exe" : "MyProxy";
            Path launcher = installDir.resolve(launcherName);

            if (Files.exists(launcher)) {
                // jpackage install: launch MyProxy.exe
                ProcessBuilder pb = new ProcessBuilder(launcher.toString());
                pb.directory(installDir.toFile());
                logger.info("Restarting via launcher: {}", launcher);
                pb.start();
            } 
            shutdownCallback.run();
        } catch (IOException e) {
            logger.error("Failed to restart application", e);
        }
    }

    /**
     * Compare two version strings in dotted format (e.g. "1.0.1" vs "1.0.0").
     *
     * @param v1 first version
     * @param v2 second version
     * @return negative if v1 < v2, zero if equal, positive if v1 > v2
     */
    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int max = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < max; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        return 0;
    }
}
