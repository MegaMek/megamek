/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.util;

import megamek.common.annotations.Nullable;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.util.Locale;

public class StringDrawer {

    private final String text;
    private int x = 0;
    private int y = 0;
    private Color fillColor = null;
    private Color outlineColor = null;
    private float outlineWidth = 0;
    private boolean centerX = false;
    private boolean centerY = false;
    private boolean rightAlign = false;
    private Font font = null;
    private double angle = 0;

    public StringDrawer(@Nullable String text) {
        this.text = text;
    }

    public StringDrawer at(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public StringDrawer color(Color color) {
        fillColor = color;
        return this;
    }

    public StringDrawer center() {
        return centerX().centerY();
    }

    public StringDrawer centerX() {
        centerX = true;
        rightAlign = false;
        return this;
    }

    public StringDrawer centerY() {
        centerY = true;
        return this;
    }

    public StringDrawer rightAlign() {
        rightAlign = true;
        centerX = false;
        return this;
    }

    public StringDrawer font(Font font) {
        this.font = font;
        return this;
    }

    public StringDrawer outline(Color color, float width) {
        outlineColor = color;
        outlineWidth = width;
        return this;
    }

    public StringDrawer useConfig(StringDrawerConfig style) {
        font = style.font;
        fillColor = style.fillColor;
        outlineColor = style.outlineColor;
        outlineWidth = style.outlineWidth;
        centerX = style.centerX;
        centerY = style.centerY;
        angle = style.angle;
        return this;
    }

    public StringDrawer rotate(double angle) {
        this.angle = angle;
        return this;
    }

    public StringDrawer fitIn(Rectangle boundary) {
        //TODO: fit in the given rectangle width/height by reducing the font; font must be given
        return this;

    }

    public StringDrawer fallBackFont(String logicalFont) {
        //TODO: add a fallback logical font
        return this;
    }

    public Rectangle draw(Graphics g) {
        if ((text == null) || text.isBlank()) {
            return new Rectangle();
        }
        Graphics2D g2D = (Graphics2D) g;
        AffineTransform oldTransform = g2D.getTransform();
        Color oldColor = g.getColor();
        Font oldFont = g.getFont();

        if (fillColor != null) {
            g.setColor(fillColor);
        }
        if (font != null) {
            g.setFont(font);
        }

        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector gv = g.getFont().createGlyphVector(frc, text);
        Rectangle bounds = gv.getPixelBounds(null, 0, 0);

        // Use the size of a pure uppercase placeholder text for Y-centering to keep the same baseline for any text
        GlyphVector gvUpperCase = g.getFont().createGlyphVector(frc, "AKMPO");
        Rectangle boundsUpperCase = gvUpperCase.getPixelBounds(null, 0, 0);

        int posX = centerX ? x - bounds.width / 2 - bounds.x : x;
        int posY = centerY ? y + boundsUpperCase.height / 2 : y;
        if (rightAlign) {
            posX = x - bounds.width - bounds.x;
        }
        if (angle != 0) {
            g2D.translate(posX + bounds.width / 2, posY - bounds.height / 2);
            g2D.rotate(angle);
            g2D.translate(-posX - bounds.width / 2, -posY + bounds.height / 2);
        }
        g.setColor(fillColor);
        g2D.fill(gv.getOutline(posX, posY));
        if (outlineWidth > 0) {
            g.setColor(outlineColor);
            g2D.setStroke(new BasicStroke(outlineWidth));
            g2D.draw(gv.getOutline(posX, posY));
        }

        // replace the width, as the glyphvector width doesn't include trailing or leading spaces
        bounds.width = g.getFontMetrics(g.getFont()).stringWidth(text);

        g2D.setTransform(oldTransform);
        g.setColor(oldColor);
        g.setFont(oldFont);
        bounds.translate(posX, posY);
        return bounds;
    }

    public static class StringDrawerConfig {
        private Color fillColor = null;
        private Color outlineColor = null;
        private float outlineWidth = 0;
        private boolean centerX = false;
        private boolean centerY = false;
        private Font font = null;
        private double angle = 0;

        public StringDrawerConfig color(Color color) {
            fillColor = color;
            return this;
        }

        public StringDrawerConfig center() {
            return centerX().centerY();
        }

        public StringDrawerConfig centerX() {
            centerX = true;
            return this;
        }

        public StringDrawerConfig centerY() {
            centerY = true;
            return this;
        }

        public StringDrawerConfig font(Font font) {
            this.font = font;
            return this;
        }

        public StringDrawerConfig outline(Color color, float width) {
            outlineColor = color;
            outlineWidth = width;
            return this;
        }

        public StringDrawerConfig rotate(double angle) {
            this.angle = angle;
            return this;
        }
    }

}
