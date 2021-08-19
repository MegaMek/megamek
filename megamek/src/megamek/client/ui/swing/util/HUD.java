/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package megamek.client.ui.swing.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.SwingConstants;


/**
 * Utility classes implementing a heads-up-display L&amp;F
 */
public class HUD {

    public final static Color FOREGROUND_COLOR = new Color(0.95f, 0.95f, 0.95f);
    public final static Color BORDER_COLOR = new Color(0.5f, 0.5f, 0.5f);
    public final static Color BACKGROUND_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.66f);
    public final static Color TRANSPARENT_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.0f);

    public final static javax.swing.border.Border BORDER = BorderFactory.createCompoundBorder(
        new HUD.Border(new Insets(1, 1, 1, 1), HUD.BORDER_COLOR, 2),
        BorderFactory.createEmptyBorder(4, 8, 4, 8)
    );
    public final static javax.swing.border.Border BORDER_EAST = BorderFactory.createCompoundBorder(
        new HUD.Border(new Insets(1, 0, 1, 1), HUD.BORDER_COLOR, 2),
        BorderFactory.createEmptyBorder(4, 8, 4, 8)
    );
    public final static javax.swing.border.Border BORDER_WEST = BorderFactory.createCompoundBorder(
        new HUD.Border(new Insets(1, 1, 1, 0), HUD.BORDER_COLOR, 2),
        BorderFactory.createEmptyBorder(4, 8, 4, 8)
    );

    public final static String BORDER_PROPERTY = "megamek.client.ui.swing.util.HUD.BORDER_PROPERTY";


    /** Provides optional, possibly rounded component line border. */
    public static class Border implements javax.swing.border.Border {

        private Insets widths;
        private Color color;
        private int radius;


        public Border(Insets widths, Color color, int radius)  {
            this.widths = widths;
            this.color = color;
            this.radius = radius;
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return (Insets) this.widths.clone();
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            final var gSub = g.create();
            try {
                final var g2 = (Graphics2D) gSub;
                g2.setColor(this.color);

                final var outer = new RoundRectangle2D.Float(
                    x, y, width, height, this.radius, this.radius
                );
                final var insets = this.widths;
                final var inner = new RoundRectangle2D.Float(
                    x + insets.left,
                    y + insets.top,
                    width - (insets.left + insets.right),
                    height - (insets.top + insets.bottom),
                    this.radius, this.radius
                );

                final var path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
                path.append(outer, false);
                path.append(inner, false);
                g2.fill(path);
            } catch (ClassCastException cce) {
                // not a Graphics2D object
            } finally {
                gSub.dispose();
            }
        }

    }

    /**
     * Applies HUD styling to a component and its descendants.
     */
    public static void applyHud(Component component) {
        boolean addBorder = false;

        if (component instanceof javax.swing.AbstractButton) {
            var button = (javax.swing.AbstractButton) component;
            button.setContentAreaFilled(false);
            addBorder = true;
        } else if (component instanceof javax.swing.text.JTextComponent) {
            var text = (javax.swing.text.JTextComponent) component;
            addBorder = true;
        } else if (component instanceof javax.swing.JScrollPane) {
            var scrollpane = (javax.swing.JScrollPane) component;
            scrollpane.setViewportBorder(null);
            applyHud(scrollpane.getViewport());
        } else if (component instanceof java.awt.Container) {
            for (var child: ((java.awt.Container) component).getComponents()) {
                applyHud(child);
            }
        }

        component.setForeground(FOREGROUND_COLOR);
        if (component instanceof javax.swing.JComponent) {
            var jcomponent = (javax.swing.JComponent) component;
            javax.swing.border.Border border = null;
            if (addBorder) {
                border = BORDER;
                var borderProp = jcomponent.getClientProperty(BORDER_PROPERTY);
                if (borderProp instanceof Integer) {
                    switch ((int) borderProp) {
                    case SwingConstants.WEST:
                    case SwingConstants.LEADING:
                        border = BORDER_WEST;
                        break;
                    case SwingConstants.EAST:
                    case SwingConstants.TRAILING:
                        border = BORDER_EAST;
                        break;
                    }
                }
            }
            jcomponent.setBorder(border);
            jcomponent.setBackground(null);
            jcomponent.setOpaque(false);
        } else {
            component.setBackground(BACKGROUND_COLOR);
        }
    }

}
