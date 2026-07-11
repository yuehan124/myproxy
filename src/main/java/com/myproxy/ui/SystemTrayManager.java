package com.myproxy.ui;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

/**
 * System tray manager for displaying notifications and handling user actions.
 * 
 * @author yuehan124@gmail.com
 * @since 2026-07-05
 */
public class SystemTrayManager {

    private final MainFrame mainFrame;
    private TrayIcon trayIcon;
    private SystemTray systemTray;
    private JPopupMenu popup;

    public SystemTrayManager(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public void setup() {
        if (!SystemTray.isSupported()) {
            return;
        }
        systemTray = SystemTray.getSystemTray();

        Image image = UiUtils.loadAppIcon(16);
        popup = createMenu();

        trayIcon = new TrayIcon(image, "MyProxy");
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    mainFrame.showWindow();
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    popup.setLocation(e.getX(), e.getY());
                    popup.setInvoker(popup);
                    popup.setVisible(true);
                }
            }
        });

        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void remove() {
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
    }

    private JPopupMenu createMenu() {
        I18nManager i18n = I18nManager.getInstance();
        JPopupMenu menu = new JPopupMenu();

        JMenuItem showItem = new JMenuItem(i18n.getString("tray.show"));
        showItem.addActionListener(e -> mainFrame.showWindow());
        menu.add(showItem);

        JMenuItem showLogItem = new JMenuItem(i18n.getString("tray.showlog"));
        showLogItem.addActionListener(e -> openLogFile());
        menu.add(showLogItem);

        JMenuItem editConfigItem = new JMenuItem(i18n.getString("tray.editconfig"));
        editConfigItem.addActionListener(e -> openConfigFile());
        menu.add(editConfigItem);

        menu.addSeparator();

        JMenuItem exitItem = new JMenuItem(i18n.getString("tray.exit"));
        exitItem.addActionListener(e -> {
            remove();
            mainFrame.exitApp();
        });
        menu.add(exitItem);

        return menu;
    }

    private void openLogFile() {
        String userHome = System.getProperty("user.home");
        File logDir = new File(userHome, ".myproxy/logs");
        File logFile = new File(logDir, "myproxy.log");
        openFile(logFile.exists() ? logFile : logDir);
    }

    private void openConfigFile() {
        String userHome = System.getProperty("user.home");
        File configFile = new File(userHome, ".myproxy/config.json");
        openFile(configFile);
    }

    private void openFile(File file) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
