/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.widget.picmap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

public class PMSimpleLabel implements PMLabel {

    // The String to display.
    String string;
    // The position of the label
    int x = 0;
    int y = 0;
    // The width and height of the label
    public int width;
    int height;
    // The descent of the label
    int descent;
    // Color to draw the label with.
    Color color;
    FontMetrics fm;

    boolean visible = true;

    /** The ring drawn around an outlined label, and whether this label has one. */
    private static final Color OUTLINE_COLOR = Color.white;
    protected boolean outlined = false;

    /** The factor the text is enlarged by, so it is drawn at that size rather than magnified and left blocky. */
    protected double drawScale = 1.0;

    /*
     * Create the label with the specified string, font and color
     */
    public PMSimpleLabel(String string, FontMetrics fontMetrics, Color color) {
        this.string = string;
        this.fm = fontMetrics;
        width = fontMetrics.stringWidth(this.string);
        height = fontMetrics.getHeight();
        descent = fontMetrics.getMaxDescent();
        this.color = color;
    }

    @Override
    public void setString(String s) {
        string = s;
        // The width use to just be the stringWidth, but this
        // sometimes caused cropping when setString was called.
        // The value of 140% was chosen by trial and error, and
        // may be incorrect. In fact, this whole fix is
        // basically a kludge, since I don't know why it
        // is needed.
        width = (int) Math.ceil(fm.stringWidth(string) * 1.4);
        height = fm.getHeight();
        descent = fm.getMaxDescent();
    }

    /*
     * Set the color of the label of the font.
     */
    @Override
    public void setColor(Color c) {
        color = c;
    }

    /*
     * translate the coordinates of the label.
     */
    @Override
    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void translate(int x, int y) {
        this.x += x;
        this.y += y;
    }

    /*
     * Draw the label.
     */
    @Override
    public void drawInto(Graphics g) {
        if (!visible) {
            return;
        }
        Font font = g.getFont();
        Color temp = g.getColor();
        g.setFont(fm.getFont());
        drawOutlinedString(g, string, x, y);
        g.setColor(temp);
        g.setFont(font);
    }

    /**
     * Draws the label, ringed with a contrasting outline when it is outlined. A label over a location striped for a
     * critical hit would otherwise have to compete with the stripes drawn underneath it.
     * <p>
     * When the label is enlarged, its position and font are scaled here and it is drawn at that size, so its text
     * renders crisply rather than being magnified through the component's transform and left blocky.
     * </p>
     */
    protected void drawOutlinedString(Graphics g, String text, int atX, int atY) {
        int drawX = atX;
        int drawY = atY;
        Font previousFont = null;
        if (drawScale != 1.0) {
            drawX = (int) Math.round(atX * drawScale);
            drawY = (int) Math.round(atY * drawScale);
            Font baseFont = fm.getFont();
            previousFont = g.getFont();
            g.setFont(baseFont.deriveFont((float) (baseFont.getSize2D() * drawScale)));
        }
        if (outlined) {
            g.setColor(OUTLINE_COLOR);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if ((dx != 0) || (dy != 0)) {
                        g.drawString(text, drawX + dx, drawY + dy);
                    }
                }
            }
        }
        g.setColor(color);
        g.drawString(text, drawX, drawY);
        if (previousFont != null) {
            g.setFont(previousFont);
        }
    }

    /** Rings the label with a contrasting outline, for when what is drawn under it would drown it out. */
    public void setOutlined(boolean outlined) {
        this.outlined = outlined;
    }

    @Override
    public void setDrawScale(double scale) {
        drawScale = scale;
    }

    @Override
    public void setVisible(boolean v) {
        visible = v;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y - height + descent, width, height);
    }

    /*
     * Returns the size of the label
     */
    @Override
    public Dimension getSize() {
        return new Dimension(width, height);
    }

    /*
     * Returns the descent of the label.
     */
    @Override
    public int getDescent() {
        return descent;
    }

}
