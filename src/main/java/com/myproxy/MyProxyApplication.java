package com.myproxy;

import com.myproxy.config.ConfigManager;
import com.myproxy.proxy.ProxyService;
import com.myproxy.proxy.ReverseProxyService;
import com.myproxy.ui.MainFrame;
import com.myproxy.ui.SystemTrayManager;
import com.myproxy.update.UpdateService;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.awt.SystemTray;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Application entry point. Responsible for initializing and starting the proxy services.
 * 
 * @author yuehan124@gmail.com
 * @since 2026-07-05
 */
public class MyProxyApplication {

    public static void main(String[] args) {
        // Ensure log directory exists before logback initializes
        try {
            Path logDir = Path.of(System.getProperty("user.home"), ".myproxy", "logs");
            Files.createDirectories(logDir);
        } catch (Exception ignored) {
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // Set unified font for all Swing components
        Font unifiedFont = new Font(Font.DIALOG, Font.PLAIN, 12);
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(unifiedFont));
            }
        }

        SwingUtilities.invokeLater(() -> {
            try {
                ConfigManager configManager = new ConfigManager();
                configManager.loadConfig();

                ProxyService proxyService = new ProxyService(configManager);
                ReverseProxyService reverseProxyService = new ReverseProxyService(configManager);

                MainFrame mainFrame = new MainFrame(proxyService, reverseProxyService, configManager);

                if (SystemTray.isSupported()) {
                    SystemTrayManager tray = new SystemTrayManager(mainFrame);
                    tray.setup();
                }

                mainFrame.setVisible(true);

                // Check for updates asynchronously
                UpdateService updateService = new UpdateService(configManager, mainFrame::exitApp);
                updateService.checkForUpdatesAsync();

                // Auto-start forward proxy on startup
                new Thread(() -> proxyService.start()).start();

                // Auto-start reverse proxy if enabled with rules
                if (configManager.getConfig().isReverseProxyEnabled()
                        && !configManager.getConfig().getReverseProxyRules().isEmpty()) {
                    new Thread(() -> reverseProxyService.start()).start();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "\u542f\u52a8\u5931\u8d25: " + e.getMessage(),
                        "\u9519\u8bef", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
