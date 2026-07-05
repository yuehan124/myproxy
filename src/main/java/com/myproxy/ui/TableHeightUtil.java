package com.myproxy.ui;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Auto-adjusts the height of a JScrollPane containing a JTable based on the number of data rows,
 * with an upper limit.
 * <p>
 * Prerequisite: Place the JScrollPane in the {@link BorderLayout#NORTH} position of the parent container,
 * so that the preferredSize height takes effect and is not stretched by BorderLayout.CENTER.
 * 
 * @author yuehan124@gmail.com
 * @since 2026-07-05
 */
public final class TableHeightUtil {

    private TableHeightUtil() {
    }

    /**
     * Adjusts the scrollPane's preferred height based on the current row count;
     * enables scrollbars when the row count exceeds maxRows.
     *
     * @param table       the table
     * @param scrollPane  the scroll pane wrapping the table
     * @param maxRows     maximum visible rows (vertical scrollbar appears when exceeded)
     */
    public static void adjustHeight(JTable table, JScrollPane scrollPane, int maxRows) {
        int rowCount = table.getRowCount();
        int rowHeight = table.getRowHeight();
        int headerHeight = table.getTableHeader() != null
                ? table.getTableHeader().getPreferredSize().height : 0;
        // Extra gap to ensure no unwanted scrollbar
        int gap = 8;
        int maxHeight = maxRows * rowHeight + headerHeight + gap;
        int contentHeight = rowCount * rowHeight + headerHeight + gap;
        int preferredHeight = Math.min(contentHeight, maxHeight);
        scrollPane.setPreferredSize(new Dimension(0, preferredHeight));
        scrollPane.revalidate();
        // Force refresh scrollbar visibility
        scrollPane.getViewport().revalidate();
    }
}
