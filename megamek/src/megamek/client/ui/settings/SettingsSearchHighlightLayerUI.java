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
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.LayerUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

/** Paints settings search matches without changing component text or layout. */
final class SettingsSearchHighlightLayerUI extends LayerUI<JScrollPane> {
    private static final int HIGHLIGHT_ALPHA = 96;
    private static final int OUTLINE_ALPHA = 180;
    private static final int HORIZONTAL_PADDING = 2;
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

        Rectangle viewportBounds = SwingUtilities.convertRectangle(viewport,
              new Rectangle(0, 0, viewport.getWidth(), viewport.getHeight()), layer);
        List<Rectangle> highlights = new ArrayList<>();
        collectHighlights(root, layer, viewportBounds, highlights);
        paintHighlights((Graphics2D) graphics, highlights);
    }

    List<Rectangle> highlightBounds(JLayer<JScrollPane> layer) {
        if (tokens.isEmpty()) {
            return List.of();
        }
        JScrollPane scrollPane = layer.getView();
        JViewport viewport = scrollPane.getViewport();
        Component root = viewport.getView();
        if (root == null) {
            return List.of();
        }
        Rectangle viewportBounds = SwingUtilities.convertRectangle(viewport,
              new Rectangle(0, 0, viewport.getWidth(), viewport.getHeight()), layer);
        List<Rectangle> highlights = new ArrayList<>();
        collectHighlights(root, layer, viewportBounds, highlights);
        return List.copyOf(highlights);
    }

    private void collectHighlights(Component component, JLayer<?> layer, Rectangle viewportBounds,
          List<Rectangle> highlights) {
        if (!component.isVisible()) {
            return;
        }
        if (component instanceof JComponent textComponent) {
            Rectangle componentBounds = SwingUtilities.convertRectangle(textComponent,
                  new Rectangle(0, 0, textComponent.getWidth(), textComponent.getHeight()), layer);
            if (!componentBounds.intersects(viewportBounds)) {
                return;
            }
            for (Rectangle textBounds : textHighlightBounds(textComponent)) {
                Rectangle layerBounds = SwingUtilities.convertRectangle(textComponent, textBounds, layer);
                Rectangle visibleBounds = layerBounds.intersection(viewportBounds);
                if (!visibleBounds.isEmpty()) {
                    highlights.add(visibleBounds);
                }
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectHighlights(child, layer, viewportBounds, highlights);
            }
        }
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
        Rectangle allocation = textAllocation(component, source);
        if (allocation.isEmpty()) {
            return List.of();
        }

        List<Rectangle> highlights = new ArrayList<>();
        for (SettingsSearchText.TextRange range : ranges) {
            Rectangle bounds = source.isHtml()
                  ? htmlRangeBounds(source, range, allocation)
                  : plainRangeBounds(component, source.text(), range, allocation);
            if (bounds != null && !bounds.isEmpty()) {
                bounds.grow(HORIZONTAL_PADDING, 0);
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
                bounds.grow(HORIZONTAL_PADDING, 0);
                highlights.add(bounds);
            } catch (BadLocationException exception) {
                // Ignore a stale document position; a later repaint recalculates from the current document.
            }
        }
        return highlights;
    }

    private static Rectangle textAllocation(JComponent component, SettingsSearchText.TextSource source) {
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
        if (source.isHtml()) {
            textBounds.width = Math.max(textBounds.width,
                  (int) Math.ceil(source.htmlView().getPreferredSpan(javax.swing.text.View.X_AXIS)));
            textBounds.height = Math.max(textBounds.height,
                  (int) Math.ceil(source.htmlView().getPreferredSpan(javax.swing.text.View.Y_AXIS)));
        }
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
        Color baseColor = UIManager.getColor("TextField.selectionBackground");
        if (baseColor == null) {
            baseColor = new Color(255, 193, 7);
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
