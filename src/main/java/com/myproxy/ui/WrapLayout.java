package com.myproxy.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * FlowLayout subclass that fully supports wrapping of components.
 */
public class WrapLayout extends FlowLayout {

    private Dimension preferredLayoutSize;

    public WrapLayout() {
        super();
    }

    public WrapLayout(int align) {
        super(align);
    }

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public void layoutContainer(Container target) {
        super.layoutContainer(target);
        preferredLayoutSize = null;
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        if (preferredLayoutSize == null) {
            preferredLayoutSize = layoutSize(target, true);
        }
        return preferredLayoutSize;
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = target.getWidth() - horizontalInsetsAndGap;

            if (maxWidth <= 0) {
                maxWidth = Integer.MAX_VALUE;
            }

            int nmembers = target.getComponentCount();
            int x = 0;
            int y = insets.top + vgap;
            int rowHeight = 0;
            int maxRowWidth = 0;

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    if (x + d.width > maxWidth && x > 0) {
                        x = 0;
                        y += rowHeight + vgap;
                        rowHeight = 0;
                    }

                    if (x != 0) {
                        x += hgap;
                    }
                    x += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                    maxRowWidth = Math.max(maxRowWidth, x);
                }
            }

            Dimension dim = new Dimension();
            dim.width = maxRowWidth + horizontalInsetsAndGap;
            dim.height = y + rowHeight + insets.bottom + vgap;
            return dim;
        }
    }
}
