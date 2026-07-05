package com.myproxy.ui;

import com.myproxy.config.ConfigManager;
import com.myproxy.config.ProxyConfig;
import com.myproxy.proxy.ProxyService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class ProxyPanel extends JPanel {

    private final ProxyService proxyService;
    private final ConfigManager configManager;

    private final JButton startButton;
    private final JButton stopButton;
    private final JTextField portField;
    private final JLabel statusLabel;

    public ProxyPanel(ProxyService proxyService, ConfigManager configManager) {
        this.proxyService = proxyService;
        this.configManager = configManager;

        I18nManager i18n = I18nManager.getInstance();

        setLayout(new WrapLayout(FlowLayout.LEFT, 8, 4));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(i18n.getString("panel.forward.title")),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        statusLabel = new JLabel(i18n.getString("status.stopped"));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        statusLabel.setForeground(UiUtils.COLOR_STOPPED);
        add(statusLabel);

        add(Box.createHorizontalStrut(15));

        add(new JLabel(i18n.getString("label.port")));
        portField = new JTextField(String.valueOf(configManager.getConfig().getPort()), 6);
        portField.addActionListener(e -> applyPort());
        portField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                applyPort();
            }
        });
        add(portField);

        startButton = new JButton(i18n.getString("button.start"));
        startButton.addActionListener(e -> new Thread(() -> proxyService.start()).start());
        add(startButton);

        stopButton = new JButton(i18n.getString("button.stop"));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> new Thread(() -> proxyService.stop()).start());
        add(stopButton);
    }

    private void applyPort() {
        I18nManager i18n = I18nManager.getInstance();
        int currentPort = configManager.getConfig().getPort();
        int port = UiUtils.parsePort(portField, currentPort);
        if (port < 0) {
            JOptionPane.showMessageDialog(this,
                    i18n.getString("message.port.invalid"), i18n.getString("message.warning"),
                    JOptionPane.WARNING_MESSAGE);
            portField.setText(String.valueOf(currentPort));
            return;
        }
        ProxyConfig config = configManager.getConfig();
        if (config.getPort() != port) {
            config.setPort(port);
            configManager.saveConfig();
        }
    }

    public void updateButtonState(boolean isRunning) {
        startButton.setEnabled(!isRunning);
        stopButton.setEnabled(isRunning);
        portField.setEnabled(!isRunning);
    }

    public void updateStatus(boolean running) {
        UiUtils.updateStatusLabel(statusLabel, running);
    }
}
