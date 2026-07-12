package com.myproxy.ui;

import com.myproxy.config.ConfigManager;
import com.myproxy.config.ProxyConfig;
import com.myproxy.proxy.ReverseProxyService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

/**
 * Panel for managing reverse proxy settings.
 * 
 * @author yuehan124@gmail.com
 * @since 2026-07-05
 */
public class ReverseProxyPanel extends JPanel {

    private final ConfigManager configManager;
    private final ReverseProxyService reverseProxyService;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField domainInput;
    private final JTextField targetInput;
    private final JTextField portField;
    private final JButton startButton;
    private final JButton stopButton;
    private final JLabel statusLabel;

    public ReverseProxyPanel(ReverseProxyService reverseProxyService, ConfigManager configManager) {
        this.configManager = configManager;
        this.reverseProxyService = reverseProxyService;

        I18nManager i18n = I18nManager.getInstance();

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(i18n.getString("panel.reverse.title")),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        // Control panel (contains status and input controls)
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        // Status bar row: status + port + start/stop
        JPanel statusRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 2));
        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = new JLabel(i18n.getString("status.stopped"));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        statusLabel.setForeground(UiUtils.COLOR_STOPPED);
        statusRow.add(statusLabel);
        statusRow.add(Box.createHorizontalStrut(15));

        statusRow.add(new JLabel(i18n.getString("label.port")));
        portField = new JTextField(String.valueOf(configManager.getConfig().getReverseProxyPort()), 6);
        portField.addActionListener(e -> applyPort());
        portField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                applyPort();
            }
        });
        statusRow.add(portField);

        startButton = new JButton(i18n.getString("button.start"));
        startButton.addActionListener(e -> new Thread(() -> reverseProxyService.start()).start());
        statusRow.add(startButton);

        stopButton = new JButton(i18n.getString("button.stop"));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> new Thread(() -> reverseProxyService.stop()).start());
        statusRow.add(stopButton);

        controlPanel.add(statusRow);
        controlPanel.add(Box.createVerticalStrut(5));

        // Horizontal separator
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        controlPanel.add(separator);
        controlPanel.add(Box.createVerticalStrut(5));

        // Domain input row
        JPanel domainRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 3));
        domainRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel domainLabel = new JLabel(i18n.getString("label.listen.domain"));

        // Target input row + action buttons
        JPanel targetRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 3));
        targetRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel targetLabel = new JLabel(i18n.getString("label.target.address"));
        Dimension labelSize = new Dimension(
                Math.max(domainLabel.getPreferredSize().width, targetLabel.getPreferredSize().width),
                Math.max(domainLabel.getPreferredSize().height, targetLabel.getPreferredSize().height));
        domainLabel.setMinimumSize(labelSize);
        domainLabel.setPreferredSize(labelSize);
        domainLabel.setMaximumSize(labelSize);
        targetLabel.setMinimumSize(labelSize);
        targetLabel.setPreferredSize(labelSize);
        targetLabel.setMaximumSize(labelSize);

        domainRow.add(domainLabel);
        domainInput = new JTextField();
        domainInput.setPreferredSize(new Dimension(UiUtils.INPUT_FIELD_WIDTH, domainInput.getPreferredSize().height));
        domainRow.add(domainInput);
        targetRow.add(targetLabel);
        targetInput = new JTextField();
        targetInput.setPreferredSize(new Dimension(UiUtils.INPUT_FIELD_WIDTH, targetInput.getPreferredSize().height));
        targetRow.add(targetInput);

        JButton addBtn = new JButton(i18n.getString("button.add"));
        addBtn.addActionListener(e -> addRule());
        targetRow.add(addBtn);

        JButton removeBtn = new JButton(i18n.getString("button.remove"));
        removeBtn.addActionListener(e -> removeRule());
        targetRow.add(removeBtn);

        controlPanel.add(domainRow);
        controlPanel.add(targetRow);
        add(controlPanel, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(new String[]{
                i18n.getString("label.rule.no"),
                i18n.getString("label.rule.domain"),
                i18n.getString("label.rule.target")
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        UiUtils.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);

        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshTable();
    }

    private void applyPort() {
        I18nManager i18n = I18nManager.getInstance();
        int currentPort = configManager.getConfig().getReverseProxyPort();
        int port = UiUtils.parsePort(portField, currentPort);
        if (port < 0) {
            JOptionPane.showMessageDialog(this,
                    i18n.getString("message.port.invalid"), i18n.getString("message.warning"),
                    JOptionPane.WARNING_MESSAGE);
            portField.setText(String.valueOf(currentPort));
            return;
        }
        configManager.getConfig().setReverseProxyPort(port);
        configManager.saveConfig();
    }

    private void addRule() {
        I18nManager i18n = I18nManager.getInstance();
        String domain = domainInput.getText().trim();
        String target = targetInput.getText().trim();
        if (domain.isEmpty() || target.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    i18n.getString("message.rule.empty"), i18n.getString("message.error"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            target = "http://" + target;
            targetInput.setText(target);
        }

        List<ProxyConfig.ReverseProxyRule> rules = configManager.getConfig().getReverseProxyRules();
        for (ProxyConfig.ReverseProxyRule r : rules) {
            if (r.getDomain().equalsIgnoreCase(domain)) {
                JOptionPane.showMessageDialog(this,
                        i18n.getString("message.rule.exists", domain), i18n.getString("message.error"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        rules.add(new ProxyConfig.ReverseProxyRule(domain, target));
        configManager.saveConfig();
        refreshTable();
        domainInput.setText("");
        targetInput.setText("");
    }

    private void removeRule() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        List<ProxyConfig.ReverseProxyRule> rules = configManager.getConfig().getReverseProxyRules();
        rules.remove(row);
        configManager.saveConfig();
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<ProxyConfig.ReverseProxyRule> rules = configManager.getConfig().getReverseProxyRules();
        int idx = 1;
        for (ProxyConfig.ReverseProxyRule rule : rules) {
            tableModel.addRow(new Object[]{idx++, rule.getDomain(), rule.getTarget()});
        }
    }

    public void updateStatus(boolean running) {
        UiUtils.updateStatusLabel(statusLabel, running);
    }

    public void updateButtonState(boolean running) {
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
        portField.setEnabled(!running);
    }
}
