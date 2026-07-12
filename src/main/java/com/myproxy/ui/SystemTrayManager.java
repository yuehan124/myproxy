package com.myproxy.ui;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

/**
 * System tray manager for displaying notifications and handling user actions.
 *
 * <p>Uses a Swing {@link JPopupMenu} rendered inside an always-on-top
 * {@link JDialog} instead of an AWT {@link java.awt.PopupMenu}. AWT native
 * menu items rely on the JVM {@code file.encoding} which on Windows may not
 * match the system ANSI code page, causing mojibake for non-ASCII text. Swing
 * menus are rendered via Java2D and are unaffected by {@code file.encoding},
 * so Chinese labels display correctly.</p>
 *
 * @author yuehan124@gmail.com
 * @since 2026-07-05
 */
public class SystemTrayManager {

    private final MainFrame mainFrame;
    private TrayIcon trayIcon;
    private SystemTray systemTray;
    private JPopupMenu popup;
    private boolean popupVisible = false;

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

        trayIcon = new TrayIcon(image, "MyProxy", null);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    mainFrame.showWindow();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // On Windows, isPopupTrigger() returns true on release
                if (e.isPopupTrigger() && !popupVisible) {
                    showPopup(e);
                }
            }
        });

        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show the Swing JPopupMenu at the given screen position.
     * Uses a transparent, always-on-top JDialog as invoker to avoid being
     * obscured by the Windows taskbar.
     */
    private void showPopup(MouseEvent e) {
        if (popup == null) {
            return;
        }
        popupVisible = true;
        SwingUtilities.invokeLater(() -> {
            Point loc = MouseInfo.getPointerInfo().getLocation();
            // Pre-calculate menu size so it pops up above the cursor,
            // similar to the native Windows tray menu behavior
            popup.pack();
            int menuHeight = popup.getPreferredSize().height;
            // Place invoker above the cursor by menu height, so that
            // popup.show(invoker, 0, 0) makes the menu bottom align with cursor
            int invokerX = loc.x;
            int invokerY = loc.y - menuHeight;
            JDialog invoker = new JDialog();
            invoker.setUndecorated(true);
            invoker.setSize(1, 1);
            invoker.setLocation(invokerX, invokerY);
            invoker.setBackground(new Color(0, 0, 0, 0));
            invoker.setType(java.awt.Window.Type.UTILITY);
            invoker.setAlwaysOnTop(true);
            invoker.setVisible(true);
            popup.show(invoker, 0, 0);
            popup.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    popup.removePopupMenuListener(this);
                    invoker.setVisible(false);
                    invoker.dispose();
                    popupVisible = false;
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                    popup.removePopupMenuListener(this);
                    invoker.setVisible(false);
                    invoker.dispose();
                    popupVisible = false;
                }
            });
        });
    }

    public void remove() {
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
    }

    private JPopupMenu createMenu() {
        I18nManager i18n = I18nManager.getInstance();
        // Use the global unified font (Font.DIALOG, 12pt)
        Font menuFont = new Font(Font.DIALOG, Font.PLAIN, 12);
        JPopupMenu menu = new JPopupMenu();
        menu.setFont(menuFont);

        menu.add(createMenuItem(i18n.getString("tray.show"), menuFont,
                e -> mainFrame.showWindow()));
        menu.add(createMenuItem(i18n.getString("tray.showlog"), menuFont,
                e -> openLogFile()));
        menu.add(createMenuItem(i18n.getString("tray.editconfig"), menuFont,
                e -> openConfigFile()));
        menu.addSeparator();
        menu.add(createMenuItem(i18n.getString("tray.exit"), menuFont,
                e -> {
                    remove();
                    mainFrame.exitApp();
                }));

        return menu;
    }

    private JMenuItem createMenuItem(String text, Font font, java.awt.event.ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(font);
        item.addActionListener(listener);
        return item;
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
