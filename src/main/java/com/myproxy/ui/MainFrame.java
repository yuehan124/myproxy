package com.myproxy.ui;

import com.myproxy.config.ConfigManager;
import com.myproxy.proxy.ProxyService;
import com.myproxy.proxy.ReverseProxyService;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.SystemTray;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application frame for the MyProxy application.
 * 
 * @author yuehan124@gmail.com
 * @since 2026-07-05
 */
public class MainFrame extends JFrame {

    private final ProxyService proxyService;
    private final ReverseProxyService reverseProxyService;
    private final LogPanel logPanel;
    private final StatusBar statusBar;

    public MainFrame(ProxyService proxyService, ReverseProxyService reverseProxyService, ConfigManager configManager) {
        super("MyProxy");
        this.proxyService = proxyService;
        this.reverseProxyService = reverseProxyService;

        // Initialize i18n and set title
        I18nManager i18n = I18nManager.getInstance();
        i18n.initialize(configManager.getConfig().getLanguage());
        setTitle(i18n.getString("app.title"));

        setSize(825, 600);
        setMinimumSize(new Dimension(825, 600));
        setLocationRelativeTo(null);
        setIconImage(UiUtils.createCircleIcon(32, 4, 4));

        // Main content panel using BorderLayout
        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

        logPanel = new LogPanel();

        // === Top: Forward proxy panel ===
        ProxyPanel proxyPanel = new ProxyPanel(proxyService, configManager);
        contentPanel.add(proxyPanel, BorderLayout.NORTH);

        proxyService.setLogConsumer(logPanel::appendLog);

        // === Center: Reverse proxy + IP whitelist (side by side, fill remaining height) ===
        ReverseProxyPanel reversePanel = new ReverseProxyPanel(reverseProxyService, configManager);
        reverseProxyService.setLogConsumer(logPanel::appendLog);

        WhitelistPanel whitelistPanel = new WhitelistPanel(configManager);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, reversePanel, whitelistPanel);
        splitPane.setResizeWeight(0.52);
        splitPane.setDividerSize(0);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        // Force 50/50 initial split after layout
        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                splitPane.setDividerLocation(0.52);
                splitPane.removeComponentListener(this);
            }
        });

        // Add top spacing
        JPanel splitPaneContainer = new JPanel(new BorderLayout());
        splitPaneContainer.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        splitPaneContainer.add(splitPane, BorderLayout.CENTER);

        contentPanel.add(splitPaneContainer, BorderLayout.CENTER);

        // === Bottom: Log panel ===
        logPanel.setPreferredSize(new Dimension(0, 200));
        contentPanel.add(logPanel, BorderLayout.SOUTH);

        // === Overall layout ===
        add(contentPanel, BorderLayout.CENTER);

        // === Status bar ===
        statusBar = new StatusBar();
        add(statusBar, BorderLayout.SOUTH);

        // Connect proxy status to status bar
        proxyService.setStatusConsumer(running -> {
            proxyPanel.updateStatus(running);
            proxyPanel.updateButtonState(running);
            statusBar.updateProxyStatus(proxyService.isRunning(), reverseProxyService.isRunning());
        });
        reverseProxyService.setStatusConsumer(running -> {
            reversePanel.updateStatus(running);
            reversePanel.updateButtonState(running);
            statusBar.updateProxyStatus(proxyService.isRunning(), reverseProxyService.isRunning());
        });
        // Initialize status bar
        statusBar.updateProxyStatus(proxyService.isRunning(), reverseProxyService.isRunning());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SystemTray.isSupported()) {
                    setVisible(false);
                } else {
                    exitApp();
                }
            }
        });
    }

    public void showWindow() {
        setVisible(true);
        setExtendedState(Frame.NORMAL);
        toFront();
        requestFocus();
    }

    public void exitApp() {
        if (proxyService.isRunning()) {
            proxyService.stop();
        }
        if (reverseProxyService.isRunning()) {
            reverseProxyService.stop();
        }
        System.exit(0);
    }
}
