package com.myproxy.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Utility class for shared UI constants and helper methods.
 */
public final class UiUtils {

    public static final Color COLOR_RUNNING = new Color(46, 139, 87);
    public static final Color COLOR_STOPPED = new Color(128, 128, 128);
    public static final Color COLOR_LABEL = new Color(102, 102, 102);
    public static final Color COLOR_ICON = new Color(220, 53, 69);

    /** Default font size for all UI components */
    public static final float DEFAULT_FONT_SIZE = 12f;

    /** Fixed width for text input fields */
    public static final int INPUT_FIELD_WIDTH = 120;

    private UiUtils() {
    }

    /**
     * Apply consistent font to a Swing component.
     */
    public static void applyFont(JComponent component) {
        component.setFont(component.getFont().deriveFont(DEFAULT_FONT_SIZE));
    }

    /**
     * Create a circular ring icon with the given size.
     *
     * @param size       image size in pixels
     * @param margin     margin around the circle
     * @param strokeWidth stroke width of the circle
     * @return generated icon image
     */
    public static Image createCircleIcon(int size, int margin, int strokeWidth) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(COLOR_ICON);
        g.setStroke(new BasicStroke(strokeWidth));
        g.drawOval(margin, margin, size - 2 * margin, size - 2 * margin);
        g.dispose();
        return img;
    }

    /**
     * Validate and parse a port number from a text field.
     *
     * @param textField  the text field to read from
     * @param currentPort the current valid port (for reset on error)
     * @return parsed port, or -1 if invalid
     */
    public static int parsePort(JTextField textField, int currentPort) {
        String text = textField.getText().trim();
        try {
            int port = Integer.parseInt(text);
            if (port < 1 || port > 65535) {
                return -1;
            }
            return port;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Configure a JTable with unified style: no grid lines, single selection.
     */
    public static void styleTable(JTable table) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(false);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
    }

    /**
     * Update a status label with running/stopped text and color.
     */
    public static void updateStatusLabel(JLabel statusLabel, boolean running) {
        I18nManager i18n = I18nManager.getInstance();
        if (running) {
            statusLabel.setText(i18n.getString("status.running"));
            statusLabel.setForeground(COLOR_RUNNING);
        } else {
            statusLabel.setText(i18n.getString("status.stopped"));
            statusLabel.setForeground(COLOR_STOPPED);
        }
    }

    /**
     * Convert a Color to HTML hex string (e.g. "#2e8b57").
     */
    public static String toHtmlColor(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
