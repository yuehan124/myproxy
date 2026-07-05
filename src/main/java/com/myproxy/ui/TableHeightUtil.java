package com.myproxy.ui;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * 根据数据行数自适应调整 JTable 所在 JScrollPane 的高度，并设置上限。
 * <p>
 * 使用前提：需将 JScrollPane 放置在父容器的 {@link BorderLayout#NORTH} 位置，
 * 这样 preferredSize 高度才会生效，而不会因 BorderLayout.CENTER 被拉伸占满。
 */
public final class TableHeightUtil {

    private TableHeightUtil() {
    }

    /**
     * 按当前行数调整 scrollPane 的首选高度；超过 maxRows 时启用滚动条。
     *
     * @param table       表格
     * @param scrollPane  包裹表格的滚动面板
     * @param maxRows     最大可见行数（超过则出现垂直滚动条）
     */
    public static void adjustHeight(JTable table, JScrollPane scrollPane, int maxRows) {
        int rowCount = table.getRowCount();
        int rowHeight = table.getRowHeight();
        int headerHeight = table.getTableHeader() != null
                ? table.getTableHeader().getPreferredSize().height : 0;
        // 添加额外的容差，确保不会出现滚动条
        int gap = 8;
        int maxHeight = maxRows * rowHeight + headerHeight + gap;
        int contentHeight = rowCount * rowHeight + headerHeight + gap;
        int preferredHeight = Math.min(contentHeight, maxHeight);
        scrollPane.setPreferredSize(new Dimension(0, preferredHeight));
        scrollPane.revalidate();
        // 强制刷新滚动条可见性
        scrollPane.getViewport().revalidate();
    }
}
