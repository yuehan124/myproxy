package com.myproxy.ui;

import com.myproxy.config.ConfigManager;
import com.myproxy.config.ProxyConfig;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Set;
import java.util.regex.Pattern;

public class WhitelistPanel extends JPanel {

    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
    private static final Pattern IPV6_SIMPLE = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

    private final ConfigManager configManager;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField ipInput;
    private final JCheckBox enableCb;

    public WhitelistPanel(ConfigManager configManager) {
        this.configManager = configManager;

        I18nManager i18n = I18nManager.getInstance();

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(i18n.getString("panel.whitelist.title")),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));

        // Top panel with checkbox and input rows
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Checkbox row
        JPanel checkboxRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 3));
        checkboxRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        enableCb = new JCheckBox(i18n.getString("checkbox.enable"),
                configManager.getConfig().isWhitelistEnabled());
        enableCb.addActionListener(e -> {
            configManager.getConfig().setWhitelistEnabled(enableCb.isSelected());
            configManager.saveConfig();
        });
        checkboxRow.add(enableCb);
        topPanel.add(checkboxRow);

        // Input row
        JPanel controlRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 3));
        controlRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlRow.add(new JLabel(i18n.getString("label.allowed.ip")));
        ipInput = new JTextField();
        ipInput.setPreferredSize(new Dimension(UiUtils.INPUT_FIELD_WIDTH, ipInput.getPreferredSize().height));
        controlRow.add(ipInput);

        JButton addBtn = new JButton(i18n.getString("button.add"));
        addBtn.addActionListener(e -> addIp());
        controlRow.add(addBtn);

        JButton removeBtn = new JButton(i18n.getString("button.remove"));
        removeBtn.addActionListener(e -> removeIp());
        controlRow.add(removeBtn);

        topPanel.add(controlRow);
        add(topPanel, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(new String[]{
                i18n.getString("label.rule.no"),
                "IP Address"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        UiUtils.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(80);

        add(new JScrollPane(table), BorderLayout.CENTER);
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        Set<String> ips = configManager.getConfig().getAllowedIps();
        int idx = 1;
        for (String ip : ips) {
            tableModel.addRow(new Object[]{idx++, ip});
        }
    }

    private void addIp() {
        I18nManager i18n = I18nManager.getInstance();
        String ip = ipInput.getText().trim();
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    i18n.getString("message.ip.empty"), i18n.getString("message.error"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!isValidIp(ip)) {
            JOptionPane.showMessageDialog(this,
                    i18n.getString("message.ip.invalid"), i18n.getString("message.error"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        ProxyConfig cfg = configManager.getConfig();
        if (cfg.getAllowedIps().contains(ip)) {
            JOptionPane.showMessageDialog(this,
                    i18n.getString("message.ip.exists"), i18n.getString("message.error"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        cfg.addAllowedIp(ip);
        configManager.saveConfig();
        refreshTable();
        ipInput.setText("");
    }

    private void removeIp() {
        I18nManager i18n = I18nManager.getInstance();
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        String ip = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
                i18n.getString("message.delete.confirm", ip), i18n.getString("message.confirm"),
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            configManager.getConfig().removeAllowedIp(ip);
            configManager.saveConfig();
            refreshTable();
        }
    }

    private boolean isValidIp(String ip) {
        return IPV4.matcher(ip).matches()
                || IPV6_SIMPLE.matcher(ip).matches()
                || "0:0:0:0:0:0:0:1".equals(ip)
                || "::1".equals(ip);
    }
}
