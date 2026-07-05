package com.myproxy.ui;

import com.myproxy.config.NetUtils;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

/**
 * Status bar for displaying proxy status and version information.
 * 
 * @author yuehan124@gmail.com
 * @since 2026-07-05
 */
public class StatusBar extends JPanel {

    private final JLabel forwardStatusLabel;
    private final JLabel reverseStatusLabel;

    public StatusBar() {
        super(new BorderLayout());

        I18nManager i18n = I18nManager.getInstance();

        // Left: Version and contact info
        JLabel versionLabel = new JLabel(i18n.getString("statusbar.version") + " v1.0");
        UiUtils.applyFont(versionLabel);
        versionLabel.setForeground(UiUtils.COLOR_LABEL);

        // Email link
        final String email = "yuehan124@gmail.com";
        JLabel emailLabel = new JLabel("<html>" + i18n.getString("statusbar.author") + " <a href='' style='text-decoration:none'>" + email + "</a></html>");
        UiUtils.applyFont(emailLabel);
        emailLabel.setForeground(UiUtils.COLOR_LABEL);
        emailLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        emailLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().mail(new URI("mailto:" + email));
                } catch (Exception ex) {
                    StringSelection selection = new StringSelection(email);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(StatusBar.this),
                            "Email copied to clipboard: " + email);
                }
            }
        });

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftPanel.add(versionLabel);
        leftPanel.add(emailLabel);
        add(leftPanel, BorderLayout.WEST);

        // Right: Local IP + proxy status
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JLabel ipLabel = new JLabel();
        UiUtils.applyFont(ipLabel);
        ipLabel.setForeground(UiUtils.COLOR_LABEL);
        String localIp = NetUtils.getLocalIp();
        ipLabel.setText(i18n.getString("statusbar.localip") + " " + localIp);
        ipLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ipLabel.setToolTipText(i18n.getString("statusbar.copy.ip"));
        ipLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                StringSelection selection = new StringSelection(localIp);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            }
        });
        rightPanel.add(ipLabel);

        forwardStatusLabel = new JLabel();
        UiUtils.applyFont(forwardStatusLabel);
        rightPanel.add(forwardStatusLabel);

        reverseStatusLabel = new JLabel();
        UiUtils.applyFont(reverseStatusLabel);
        rightPanel.add(reverseStatusLabel);

        add(rightPanel, BorderLayout.EAST);
    }

    public void updateProxyStatus(boolean forwardRunning, boolean reverseRunning) {
        I18nManager i18n = I18nManager.getInstance();
        String forwardText = forwardRunning ? i18n.getString("status.running") : i18n.getString("status.stopped");
        String reverseText = reverseRunning ? i18n.getString("status.running") : i18n.getString("status.stopped");
        String forwardColor = UiUtils.toHtmlColor(forwardRunning ? UiUtils.COLOR_RUNNING : UiUtils.COLOR_STOPPED);
        String reverseColor = UiUtils.toHtmlColor(reverseRunning ? UiUtils.COLOR_RUNNING : UiUtils.COLOR_STOPPED);

        forwardStatusLabel.setText("<html>" + i18n.getString("statusbar.forward") + " <font color='" + forwardColor + "'>" + forwardText + "</font></html>");
        reverseStatusLabel.setText("<html>" + i18n.getString("statusbar.reverse") + " <font color='" + reverseColor + "'>" + reverseText + "</font></html>");
    }
}
