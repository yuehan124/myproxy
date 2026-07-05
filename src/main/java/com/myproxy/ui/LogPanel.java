package com.myproxy.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogPanel extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_LINES = 2000;

    private final JTextArea logArea;
    private final JScrollPane logScrollPane;

    public LogPanel() {
        I18nManager i18n = I18nManager.getInstance();

        setLayout(new BorderLayout(0, 5));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Title
        JLabel titleLabel = new JLabel(i18n.getString("panel.log.title"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        add(titleLabel, BorderLayout.NORTH);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.setBorder(BorderFactory.createEtchedBorder());
        add(logScrollPane, BorderLayout.CENTER);
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String line = "[" + LocalDateTime.now().format(DT_FMT) + "] " + message + "\n";
            logArea.append(line);

            // Trim old lines
            if (logArea.getLineCount() > MAX_LINES) {
                try {
                    int end = logArea.getLineStartOffset(logArea.getLineCount() - MAX_LINES);
                    logArea.replaceRange("", 0, end);
                } catch (Exception ignored) {
                }
            }

            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
