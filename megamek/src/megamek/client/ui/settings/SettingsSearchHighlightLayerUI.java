/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.settings;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.LayerUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/** Paints settings search matches without changing component text or layout. */
final class SettingsSearchHighlightLayerUI extends LayerUI<JScrollPane> {
    static final String HIGHLIGHT_COLOR_KEY = "Settings.searchHighlight";
    private static final Color DEFAULT_HIGHLIGHT_COLOR = new Color(0x7F, 0xCF, 0x43);
    private static final int HIGHLIGHT_ALPHA = 160;
    private static final int OUTLINE_ALPHA = 255;
    private static final int ARC_SIZE = 4;

    private List<String> tokens = List.of();

    void setFilter(String normalizedFilter, JLayer<JScrollPane> layer) {
        tokens = SettingsSearchText.tokens(normalizedFilter);
        layer.repaint();
    }

    @Override
    public void paint(Graphics graphics, JComponent component) {
        super.paint(graphics, component);
        if (tokens.isEmpty() || !(component instanceof JLayer<?> layer)
              || !(layer.getView() instanceof JScrollPane scrollPane)) {
            return;
        }
        JViewport viewport = scrollPane.getViewport();
        Component root = viewport.getView();
        if (root == null) {
            return;
        }
        paintHighlights((Graphics2D) graphics,
              collectHighlightBounds(layer, scrollPane, graphics.getClipBounds()));
    }

    List<Rectangle> highlightBounds(JLayer<JScrollPane> layer) {
        return List.copyOf(collectHighlightBounds(layer, layer.getView(), null));
    }

    private List<Rectangle> collectHighlightBounds(JLayer<?> layer, JScrollPane scrollPane,
          Rectangle paintClip) {
        if (tokens.isEmpty()) {
            return List.of();
        }
        JViewport viewport = scrollPane.getViewport();
        Component root = viewport.getView();
        if (root == null) {
            return List.of();
        }
        Rectangle viewportBounds = SwingUtilities.convertRectangle(viewport,
              new Rectangle(0, 0, viewport.getWidth(), viewport.getHeight()), layer);
        if (paintClip != null) {
            viewportBounds = viewportBounds.intersection(paintClip);
            if (viewportBounds.isEmpty()) {
                return List.of();
            }
        }
        List<Rectangle> highlights = new ArrayList<>();
        collectComponentHighlights(root, layer, viewportBounds, highlights,
              Collections.newSetFromMap(new IdentityHashMap<>()));
        return highlights;
    }

    private void collectComponentHighlights(Component component, JLayer<?> layer, Rectangle viewportBounds,
          List<Rectangle> highlights, Set<Component> visited) {
        if (!component.isVisible() || !visited.add(component)) {
            return;
        }
        if (component instanceof JTable table) {
            if (intersectsViewport(table, layer, viewportBounds)) {
                collectTableHighlights(table, layer, viewportBounds, highlights);
            }
            JTableHeader header = table.getTableHeader();
            if (header != null && header.isVisible() && SwingUtilities.isDescendingFrom(header, layer)) {
                if (visited.add(header) && intersectsViewport(header, layer, viewportBounds)) {
                    collectTableHeaderHighlights(header, layer, viewportBounds, highlights);
                }
            }
            return;
        }
        if (component instanceof JTableHeader header) {
            if (intersectsViewport(header, layer, viewportBounds)) {
                collectTableHeaderHighlights(header, layer, viewportBounds, highlights);
            }
            return;
        }
        if (component instanceof JComponent textComponent) {
            Rectangle componentBounds = SwingUtilities.convertRectangle(textComponent,
                  new Rectangle(0, 0, textComponent.getWidth(), textComponent.getHeight()), layer);
            if (!componentBounds.intersects(viewportBounds)) {
                return;
            }
            for (Rectangle textBounds : textHighlightBounds(textComponent)) {
                Rectangle visibleBounds = clipToVisibleViewports(textComponent, textBounds, layer, viewportBounds);
                if (!visibleBounds.isEmpty()) {
                    highlights.add(visibleBounds);
                }
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectComponentHighlights(child, layer, viewportBounds, highlights, visited);
            }
        }
    }

    private static boolean intersectsViewport(JComponent component, JLayer<?> layer, Rectangle viewportBounds) {
        Rectangle componentBounds = SwingUtilities.convertRectangle(component,
              new Rectangle(0, 0, component.getWidth(), component.getHeight()), layer);
        return componentBounds.intersects(viewportBounds);
    }

    private void collectTableHighlights(JTable table, JLayer<?> layer, Rectangle outerViewportBounds,
          List<Rectangle> highlights) {
        if (table.getRowCount() == 0 || table.getColumnCount() == 0) {
            return;
        }
        Rectangle tableViewport = table.getVisibleRect();
        if (tableViewport.isEmpty()) {
            return;
        }
        int firstRow = visibleRowAt(table, new Point(tableViewport.x, tableViewport.y), 0);
        int lastRow = visibleRowAt(table,
              new Point(tableViewport.x, tableViewport.y + Math.max(0, tableViewport.height - 1)),
              table.getRowCount() - 1);
        int firstColumn = visibleColumnAt(table, new Point(tableViewport.x, tableViewport.y), 0);
        int lastColumn = visibleColumnAt(table,
              new Point(tableViewport.x + Math.max(0, tableViewport.width - 1), tableViewport.y),
              table.getColumnCount() - 1);
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                Rectangle cellBounds = table.getCellRect(row, column, false);
                if (!cellBounds.intersects(tableViewport)) {
                    continue;
                }
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component rendererComponent = table.prepareRenderer(renderer, row, column);
                collectRendererHighlights(rendererComponent, cellBounds, table, tableViewport,
                      layer, outerViewportBounds, highlights);
            }
        }
    }

    private void collectTableHeaderHighlights(JTableHeader header, JLayer<?> layer, Rectangle outerViewportBounds,
          List<Rectangle> highlights) {
        JTable table = header.getTable();
        if (table == null || table.getColumnCount() == 0) {
            return;
        }
        Rectangle headerViewport = header.getVisibleRect();
        if (headerViewport.isEmpty()) {
            return;
        }
        for (int column = 0; column < table.getColumnCount(); column++) {
            Rectangle headerBounds = header.getHeaderRect(column);
            if (!headerBounds.intersects(headerViewport)) {
                continue;
            }
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            TableCellRenderer renderer = tableColumn.getHeaderRenderer();
            if (renderer == null) {
                renderer = header.getDefaultRenderer();
            }
            Component rendererComponent = renderer.getTableCellRendererComponent(table,
                  tableColumn.getHeaderValue(), false, false, -1, column);
            collectRendererHighlights(rendererComponent, headerBounds, header, headerViewport,
                  layer, outerViewportBounds, highlights);
        }
    }

    private void collectRendererHighlights(Component rendererComponent, Rectangle cellBounds,
          JComponent tableComponent, Rectangle tableViewport, JLayer<?> layer, Rectangle outerViewportBounds,
          List<Rectangle> highlights) {
        if (!(rendererComponent instanceof JComponent rendererRoot)) {
            return;
        }
        rendererRoot.setBounds(0, 0, cellBounds.width, cellBounds.height);
        layoutRendererTree(rendererRoot);
        List<Rectangle> rendererHighlights = new ArrayList<>();
        collectRendererTextHighlights(rendererRoot, rendererRoot, rendererHighlights);
        for (Rectangle rendererHighlight : rendererHighlights) {
            rendererHighlight.translate(cellBounds.x, cellBounds.y);
            Rectangle clippedToTable = rendererHighlight.intersection(cellBounds).intersection(tableViewport);
            if (clippedToTable.isEmpty()) {
                continue;
            }
            Rectangle layerBounds = clipToVisibleViewports(tableComponent, clippedToTable,
                  layer, outerViewportBounds);
            if (!layerBounds.isEmpty()) {
                highlights.add(layerBounds);
            }
        }
    }

    private static Rectangle clipToVisibleViewports(JComponent source, Rectangle sourceBounds,
          JLayer<?> layer, Rectangle outerViewportBounds) {
        Rectangle result = SwingUtilities.convertRectangle(source, sourceBounds, layer)
              .intersection(outerViewportBounds);
        Component ancestor = source.getParent();
        while (!result.isEmpty() && ancestor != null && ancestor != layer) {
            if (ancestor instanceof JViewport viewport) {
                Rectangle viewportBounds = SwingUtilities.convertRectangle(viewport,
                      new Rectangle(0, 0, viewport.getWidth(), viewport.getHeight()), layer);
                result = result.intersection(viewportBounds);
            }
            ancestor = ancestor.getParent();
        }
        return result;
    }

    private void collectRendererTextHighlights(Component component, JComponent rendererRoot,
          List<Rectangle> highlights) {
        if (!component.isVisible()) {
            return;
        }
        if (component instanceof JComponent textComponent) {
            for (Rectangle bounds : textHighlightBounds(textComponent)) {
                Rectangle rendererBounds = textComponent == rendererRoot
                      ? bounds
                      : SwingUtilities.convertRectangle(textComponent, bounds, rendererRoot);
                highlights.add(rendererBounds);
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectRendererTextHighlights(child, rendererRoot, highlights);
            }
        }
    }

    private static void layoutRendererTree(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) {
                layoutRendererTree(container);
            }
        }
    }

    private static int visibleRowAt(JTable table, Point point, int fallback) {
        int row = table.rowAtPoint(point);
        return row < 0 ? fallback : row;
    }

    private static int visibleColumnAt(JTable table, Point point, int fallback) {
        int column = table.columnAtPoint(point);
        return column < 0 ? fallback : column;
    }

    private List<Rectangle> textHighlightBounds(JComponent component) {
        SettingsSearchText.TextSource source = SettingsSearchText.from(component);
        List<SettingsSearchText.TextRange> ranges = SettingsSearchText.ranges(source.text(), tokens);
        if (ranges.isEmpty()) {
            return List.of();
        }
        if (component instanceof JEditorPane editorPane && !editorPane.isEditable()) {
            return editorRangeBounds(editorPane, ranges);
        }
        Rectangle allocation = textAllocation(component);
        if (allocation.isEmpty()) {
            return List.of();
        }

        List<Rectangle> highlights = new ArrayList<>();
        for (SettingsSearchText.TextRange range : ranges) {
            Rectangle bounds = source.isHtml()
                  ? htmlRangeBounds(source, range, allocation)
                  : plainRangeBounds(component, source.text(), range, allocation);
            if (bounds != null && !bounds.isEmpty()) {
                highlights.add(bounds);
            }
        }
        return highlights;
    }

    private static List<Rectangle> editorRangeBounds(JEditorPane editorPane,
          List<SettingsSearchText.TextRange> ranges) {
        List<Rectangle> highlights = new ArrayList<>();
        for (SettingsSearchText.TextRange range : ranges) {
            try {
                Rectangle2D start = editorPane.modelToView2D(range.start());
                Rectangle2D end = editorPane.modelToView2D(range.end());
                if (start == null || end == null) {
                    continue;
                }
                Rectangle bounds = start.getBounds();
                if (start.getY() == end.getY()) {
                    bounds.add(end);
                } else {
                    bounds.width = Math.max(1, editorPane.getWidth() - bounds.x - editorPane.getInsets().right);
                }
                highlights.add(bounds);
            } catch (BadLocationException exception) {
                // Ignore a stale document position; a later repaint recalculates from the current document.
            }
        }
        return highlights;
    }

    private static Rectangle textAllocation(JComponent component) {
        String text;
        Icon icon;
        int verticalAlignment;
        int horizontalAlignment;
        int verticalTextPosition;
        int horizontalTextPosition;
        int iconTextGap;
        if (component instanceof JLabel label) {
            text = label.getText();
            icon = label.getIcon();
            verticalAlignment = label.getVerticalAlignment();
            horizontalAlignment = label.getHorizontalAlignment();
            verticalTextPosition = label.getVerticalTextPosition();
            horizontalTextPosition = label.getHorizontalTextPosition();
            iconTextGap = label.getIconTextGap();
        } else if (component instanceof AbstractButton button) {
            text = button.getText();
            icon = buttonIcon(button);
            verticalAlignment = button.getVerticalAlignment();
            horizontalAlignment = button.getHorizontalAlignment();
            verticalTextPosition = button.getVerticalTextPosition();
            horizontalTextPosition = button.getHorizontalTextPosition();
            iconTextGap = button.getIconTextGap();
        } else {
            return new Rectangle();
        }

        Insets insets = component.getInsets();
        Rectangle viewBounds = new Rectangle(insets.left, insets.top,
              Math.max(0, component.getWidth() - insets.left - insets.right),
              Math.max(0, component.getHeight() - insets.top - insets.bottom));
        Rectangle iconBounds = new Rectangle();
        Rectangle textBounds = new Rectangle();
        SwingUtilities.layoutCompoundLabel(component, component.getFontMetrics(component.getFont()), text, icon,
              verticalAlignment, horizontalAlignment, verticalTextPosition, horizontalTextPosition,
              viewBounds, iconBounds, textBounds, iconTextGap);
        return textBounds;
    }

    private static Icon buttonIcon(AbstractButton button) {
        if (button.getIcon() != null) {
            return button.getIcon();
        }
        if (button instanceof JCheckBox) {
            return UIManager.getIcon("CheckBox.icon");
        }
        if (button instanceof JRadioButton) {
            return UIManager.getIcon("RadioButton.icon");
        }
        return null;
    }

    private static Rectangle plainRangeBounds(JComponent component, String text, SettingsSearchText.TextRange range,
          Rectangle allocation) {
        FontMetrics metrics = component.getFontMetrics(component.getFont());
        int start = Math.min(range.start(), text.length());
        int end = Math.min(range.end(), text.length());
        int x = allocation.x + metrics.stringWidth(text.substring(0, start));
        int width = metrics.stringWidth(text.substring(start, end));
        return new Rectangle(x, allocation.y, Math.max(1, width), allocation.height);
    }

    private static Rectangle htmlRangeBounds(SettingsSearchText.TextSource source,
          SettingsSearchText.TextRange range, Rectangle allocation) {
        try {
            Shape shape = source.htmlView().modelToView(range.start(), Position.Bias.Forward,
                  range.end(), Position.Bias.Backward, allocation);
            if (shape == null) {
                return null;
            }
            return new Area(shape).getBounds();
        } catch (BadLocationException exception) {
            return null;
        }
    }

    private static void paintHighlights(Graphics2D graphics, List<Rectangle> highlights) {
        if (highlights.isEmpty()) {
            return;
        }
        Color baseColor = UIManager.getColor(HIGHLIGHT_COLOR_KEY);
        if (baseColor == null) {
            baseColor = DEFAULT_HIGHLIGHT_COLOR;
        }
        Color fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), HIGHLIGHT_ALPHA);
        Color outlineColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), OUTLINE_ALPHA);
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            for (Rectangle bounds : highlights) {
                copy.setColor(fillColor);
                copy.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, ARC_SIZE, ARC_SIZE);
                copy.setColor(outlineColor);
                copy.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, ARC_SIZE, ARC_SIZE);
            }
        } finally {
            copy.dispose();
        }
    }
}
