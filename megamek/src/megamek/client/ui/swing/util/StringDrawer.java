/*
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
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

/**
 * This is a helper class to ease the process of drawing Strings in Java Graphics2D.
 * <BR><BR>
 * Use: Construct a new StringDrawer with the target String as parameter. The constructor
 * and most other methods return the StringDrawer so further settings can be chained to
 * the constructor call, such as {@link #at(int, int)} setting the position, or {@link #color(Color)}
 * setting the fill color of the drawn text. The chain can be finished by using {@link #draw(Graphics)} which
 * will draw the string using its settings.
 * <BR><BR>
 * Example:
 * <BR><code>new StringDrawer("My Text").at(836, 470).center().font(myFont).draw(g);</code>
 * <BR><BR>
 * This class also provides a StringDrawerConfig class that can be used in a similar way to assemble
 * settings to a config that can then be used in the StrinDrawer chain to apply those settings using
 * {@link #useConfig(StringDrawerConfig)}. Note that all settings applied from a config in this way
 * can be overridden again by calling a different setting afterwards.
 * <BR><BR>
 * Example:
 * <code>
 * <BR>StringDrawer.StringDrawerConfig myConfig = new StringDrawer.StringDrawerConfig().rightAlign().centerY()
 *                 .color(Color.RED).font(myFont);
 * <BR><BR>new StringDrawer("Another Text").at(736, 553).useConfig(myConfig).draw(g);
 * </code>
 *
 * @author Simon (Juliez)
 */
public class StringDrawer {

    private final String text;
    private int x = 0;
    private int y = 0;
    private Color fillColor = null;
    private Color outlineColor = null;
    private float outlineWidth = 0;
    private Color dualOutlineColor = null;
    private float dualOutlineWidth = 0;
    private boolean centerX = false;
    private boolean centerY = false;
    private boolean rightAlign = false;
    private Font font = null;
    private float fontSize = -1;
    private double angle = 0;
    private int maxWidth = Integer.MAX_VALUE;
    private float scaleX = 1;
    private boolean drawHelpLine = false;

    /**
     * Returns a new StringDrawer with the given text to draw. Note that the StringDrawer can be used
     * multiple times but the text cannot be changed.
     *
     * @param text The text to draw when using {@link #draw(Graphics)}
     */
    public StringDrawer(@Nullable String text) {
        this.text = text;
    }

    /**
     * Sets the coordinates to draw the text at. The exact placement depends on other settings
     * such as given by {@link #center()}, {@link #centerX()}, or {@link #rightAlign()}.
     *
     * @param x The x coordinate to place the text at
     * @param y The y coordinate to place the text at
     * @return The StringDrawer itself
     */
    public StringDrawer at(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Sets the coordinates to draw the text at. The exact placement depends on other settings
     * such as given by {@link #center()}, {@link #centerX()}, or {@link #rightAlign()}.
     *
     * @param point The Point with the x and y coordinates to place the text at
     * @return The StringDrawer itself
     */
    public StringDrawer at(Point point) {
        return at(point.x, point.y);
    }

    /**
     * Sets the fill color to draw the text with. Unless {@link #outline(Color, float)} is used,
     * this the fill color fills the entire text.
     *
     * @param color The fill color of the text
     * @return The StringDrawer itself
     */
    public StringDrawer color(Color color) {
        fillColor = color;
        return this;
    }

    /**
     * Sets the StringDrawer to center the text on the coordinate given by {@link #at(int, int)}.
     * This is equivalent to calling both {@link #centerX()} and {@link #centerY()}. Note that
     * vertical centering works in a special fashion explained in {@link #centerY()}.
     *
     * @return The StringDrawer itself
     */
    public StringDrawer center() {
        return centerX().centerY();
    }

    /**
     * Sets the StringDrawer to center the text horizontally on the x coordinate given by
     * {@link #at(int, int)}. Note that StringDrawer uses GlyphVectors to draw the Strings. As
     * a result, leading or trailing spaces of the text will be ignored for centering purposes.
     *
     * @return The StringDrawer itself
     */
    public StringDrawer centerX() {
        centerX = true;
        rightAlign = false;
        return this;
    }

    /**
     * Sets the StringDrawer to center the text vertically on the y coordinate given by
     * {@link #at(int, int)}.
     * <BR><BR>
     * Note: Vertical text centering does *not* use the actual text to perform centering as this
     * would make it very difficult to keep a common baseline for multiple consecutive texts when these
     * texts contain characters with varying ascent and descent, such as "A1" and "jg". Instead,
     * vertical centering is done using a placeholder text of capital letters. Therefore, vertical
     * centering will center the text approximately at half the ascent (roughly half the height of
     * capital letters).
     *
     * @return The StringDrawer itself
     */
    public StringDrawer centerY() {
        centerY = true;
        return this;
    }

    /**
     * Sets the StringDrawer to right-align the text horizontally at the coordinate given by
     * {@link #at(int, int)}.
     *
     * @return The StringDrawer itself
     */
    public StringDrawer rightAlign() {
        rightAlign = true;
        centerX = false;
        return this;
    }

    /**
     * Sets the StringDrawer to left-align the text horizontally at the coordinate given by
     * {@link #at(int, int)}. This is the default setting. This method can be used to override
     * other alignment settings given by a previous {@link #useConfig(StringDrawerConfig)}.
     *
     * @return The StringDrawer itself
     */
    public StringDrawer leftAlign() {
        rightAlign = false;
        centerX = false;
        return this;
    }

    /**
     * Sets the StringDrawer to use the given font when drawing the String.
     *
     * @return The StringDrawer itself
     */
    public StringDrawer font(Font font) {
        this.font = font;
        return this;
    }

    /**
     * Sets the StringDrawer to use the given fontSize when drawing the String. When drawing the string,
     * a new Font object will be created using this fontSize and either the font given through
     * {@link #font(Font)} or, when no font is given, the current font. Note that when this
     * method is used, and a font is also specified, the fontSize given here takes precedence even
     * when {@link #font(Font)} is called later. Also note that {@link #maxWidth(int)} may scale down
     * the resulting font size even when this method is used (in other words, this method sets the
     * maximum possible font size). Values for fontSize <= 0 are ignored.
     *
     * @return The StringDrawer itself
     */
    public StringDrawer fontSize(float fontSize) {
        this.fontSize = fontSize <= 0 ? -1 : fontSize;
        return this;
    }

    /**
     * Sets the StringDrawer to give the text an outline of the given color and line width. Note
     * that the outline is drawn first so its width may appear smaller than expected.
     *
     * @param color The color to draw the outline in
     * @param width The brush stroke width to use
     * @return The StringDrawer itself
     */
    public StringDrawer outline(Color color, float width) {
        outlineColor = color;
        outlineWidth = width;
        return this;
    }

    /**
     * Sets the StringDrawer to give the text a second outline of the given color and line width. Note
     * that the second outline is drawn first (before {@link #outline(Color, float)}) so its width may appear
     * smaller than expected and a width value greater than that of the first outline should be
     * used. Apart from the order of drawing this outline and {@link #outline(Color, float)} are
     * equivalent.
     *
     * @param color The color to draw the outline in
     * @param width The brush stroke width to use
     * @return The StringDrawer itself
     */
    public StringDrawer dualOutline(Color color, float width) {
        dualOutlineColor = color;
        dualOutlineWidth = width;
        return this;
    }

    /**
     * Sets the StringDrawer to keep the text within the given maximum pixel width. If the text would appear
     * wider, a smaller font size is calculated and used instead. Note that the width is not strictly
     * enforced, the result may still be wider or smaller in the range of a few pixels.
     *
     * @param maxWidth The maximum width the text should have
     * @return The StringDrawer itself
     */
    public StringDrawer maxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    /**
     * Sets the StringDrawer to draw a line showing the extent of the maximum width if such a width is set.
     *
     * @return The StringDrawer itself
     */
    public StringDrawer showExtent() {
        drawHelpLine = true;
        return this;
    }

    /**
     * Sets the StringDrawer to scale the text horizontally by the given scaleX value. Values bigger than
     * 1 stretch the text, values smaller than 1 compress it. Note that the maxWidth value is not scaled
     * so scaleX < 1 will make the text fit maxWidth easier while scaleX > 1 will make it more easily reach
     * maxWidth.
     *
     * @param scaleX the horizontal stretch or compression factor to apply to the text
     * @return The StringDrawer itself
     */
    public StringDrawer scaleX(float scaleX) {
        this.scaleX = scaleX;
        return this;
    }

    /**
     * Sets the StringDrawer to use the values of the given style StringDrawerConfig. This uses the values
     * font, color, outline and dual outline, centering and right-align and rotation angle.
     *
     * @param style The StringDrawerConfig to use
     * @return The StringDrawer itself
     */
    public StringDrawer useConfig(StringDrawerConfig style) {
        font = style.font;
        fillColor = style.fillColor;
        outlineColor = style.outlineColor;
        outlineWidth = style.outlineWidth;
        dualOutlineColor = style.dualOutlineColor;
        dualOutlineWidth = style.dualOutlineWidth;
        scaleX = style.scaleX;
        centerX = style.centerX;
        rightAlign = style.rightAlign;
        centerY = style.centerY;
        angle = style.angle;
        return this;
    }

    /**
     * Sets the StringDrawer to rotate the text by the given angle (radians).
     *
     * @param angle The rotation angle in rad
     * @return The StringDrawer itself
     */
    public StringDrawer rotate(double angle) {
        this.angle = angle;
        return this;
    }

    /**
     * Draws the String with applied settings to the given Graphics g (obtained from a
     * BufferedImage, JComponent, SVG context or other source). Note that when the
     * text is empty or null, no action at all is taken. The settings of
     * the Graphics context (brush, color, transform, font) are preserved. The returned
     * Rectangle gives the size of the drawn string including leading and trailing white space.
     * Note that when no font is actively set, the used font depends on what is currently set in the
     * Graphics object. The same is true for the text color.
     *
     * @param g The Graphics context to draw to.
     * @return A Rectangle containing the size of the drawn string
     */
    public Rectangle draw(Graphics g) {
        if ((text == null) || text.isBlank()) {
            return new Rectangle();
        }
        Graphics2D g2D = (Graphics2D) g.create();
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        if (fillColor != null) {
            g2D.setColor(fillColor);
        }

        if (font != null) {
            g2D.setFont(font);
        }

        if (fontSize > 0) {
            g2D.setFont(g2D.getFont().deriveFont(fontSize));
        }

        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector gv = g2D.getFont().createGlyphVector(frc, text);
        Rectangle bounds = gv.getPixelBounds(null, 0, 0);

        if (scaleX * bounds.width > maxWidth) {
            float newSize = (float) g2D.getFont().getSize() * maxWidth / scaleX / bounds.width;
            g2D.setFont(g2D.getFont().deriveFont(newSize));
            gv = g2D.getFont().createGlyphVector(frc, text);
            bounds = gv.getPixelBounds(null, 0, 0);
        }

        // Use the size of a pure uppercase placeholder text for Y-centering to keep the same baseline for any text
        GlyphVector gvUpperCase = g2D.getFont().createGlyphVector(frc, "AKMPO");
        Rectangle boundsUpperCase = gvUpperCase.getPixelBounds(null, 0, 0);

        int posX = centerX ? x - bounds.width / 2 - bounds.x : x;
        int posY = centerY ? y + boundsUpperCase.height / 2 : y;
        if (rightAlign) {
            posX = x - bounds.width - bounds.x;
        }

        if (angle != 0) {
            g2D.translate(x, y);
            g2D.rotate(angle);
            g2D.translate(-x, -y);
        }

        Graphics2D transformedG = (Graphics2D) g2D.create();
        AffineTransform scaling = AffineTransform.getTranslateInstance(posX, posY);
        scaling.scale(scaleX, 1);
        scaling.translate(-posX, -posY);
        transformedG.transform(scaling);

        // Get the real (transformed) bounds of the text for returning
        bounds = scaling.createTransformedShape(bounds).getBounds();

        if (dualOutlineWidth > 0) {
            transformedG.setColor(dualOutlineColor);
            transformedG.setStroke(new BasicStroke(dualOutlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            transformedG.draw(gv.getOutline(posX, posY));
        }

        if (outlineWidth > 0) {
            transformedG.setColor(outlineColor);
            transformedG.setStroke(new BasicStroke(outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            transformedG.draw(gv.getOutline(posX, posY));
        }

        transformedG.setColor(fillColor);
        transformedG.fill(gv.getOutline(posX, posY));
        transformedG.dispose();


        if (drawHelpLine) {
            Graphics2D untransformedG = (Graphics2D) g.create();
            untransformedG.setStroke(new BasicStroke(1f));
            untransformedG.setColor(Color.GREEN);
            untransformedG.drawLine(x - 4, y - 4, x + 4, y + 4);
            untransformedG.drawLine(x + 4, y - 4, x - 4, y + 4);
            untransformedG.dispose();

            g2D.setStroke(new BasicStroke(1f));
            if (centerX) {
                g2D.setColor(Color.RED);
                g2D.drawLine(x, y, x - maxWidth / 2, y);
                g2D.drawLine(x - maxWidth / 2, y - 5, x - maxWidth / 2, y + 5);
                g2D.setColor(Color.BLUE);
                g2D.drawLine(x, y, x + maxWidth / 2, y);
                g2D.drawLine(x + maxWidth / 2, y - 5, x + maxWidth / 2, y + 5);
            } else if (rightAlign) {
                g2D.setColor(Color.RED);
                g2D.drawLine(x, y, x - maxWidth, y);
                g2D.drawLine(x - maxWidth, y - 5, x - maxWidth, y + 5);
            } else {
                g2D.setColor(Color.BLUE);
                g2D.drawLine(x, y, x + maxWidth, y);
                g2D.drawLine(x + maxWidth, y - 5, x + maxWidth, y + 5);
            }
        }

        g2D.dispose();
        bounds.translate(posX, posY);
        return bounds;
    }

    public static class StringDrawerConfig {
        private Color fillColor = null;
        private Color outlineColor = null;
        private float outlineWidth = 0;
        private Color dualOutlineColor = null;
        private float dualOutlineWidth = 0;
        private float scaleX = 1;
        private boolean centerX = false;
        private boolean rightAlign = false;
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

        public StringDrawerConfig rightAlign() {
            rightAlign = true;
            centerX = false;
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

        /**
         * Sets the Configuration to scale the text horizontally by the given scaleX value. Values bigger than
         * 1 stretch the text, values smaller than 1 compress it.
         *
         * @param scaleX the horizontal stretch or compression factor to apply to the text
         * @return The StringDrawerConfig itself
         */
        public StringDrawerConfig scaleX(float scaleX) {
            this.scaleX = scaleX;
            return this;
        }

        public StringDrawerConfig outline(Color color, float width) {
            outlineColor = color;
            outlineWidth = width;
            return this;
        }

        public StringDrawerConfig dualOutline(Color color, float width) {
            dualOutlineColor = color;
            dualOutlineWidth = width;
            return this;
        }

        public StringDrawerConfig rotate(double angle) {
            this.angle = angle;
            return this;
        }
    }

}
