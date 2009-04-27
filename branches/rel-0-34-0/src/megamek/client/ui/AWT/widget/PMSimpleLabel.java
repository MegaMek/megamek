/**
 * MegaMek - Copyright (C) 2000,2001,2002,2004 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.client.ui.AWT.widget;

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
    int width;
    int height;
    // The descent of the label
    int descent;
    // Color to draw the label with.
    Color color;
    // Font and Fontmetrics for the label
    Font f;
    FontMetrics fm;

    boolean visible = true;

    /*
     * Create the label with the specified string, font and color
     */
    public PMSimpleLabel(String s, FontMetrics fm, Color c) {
        string = s;
        this.fm = fm;
        width = fm.stringWidth(string);
        height = fm.getHeight();
        descent = fm.getMaxDescent();
        color = c;
    }

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
    public void setColor(Color c) {
        color = c;
    }

    /*
     * translate the coordinates of the label.
     */
    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void translate(int x, int y) {
        this.x += x;
        this.y += y;
    }

    /*
     * Draw the label.
     */
    public void drawInto(Graphics g) {
        if (!visible)
            return;
        Font font = g.getFont();
        Color temp = g.getColor();
        g.setColor(color);
        g.setFont(fm.getFont());
        g.drawString(string, x, y);
        g.setColor(temp);
        g.setFont(font);
    }

    public void setVisible(boolean v) {
        visible = v;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y - height + descent, width, height);
    }

    /*
     * Returns the size of the label
     */
    public Dimension getSize() {
        return new Dimension(width, height);
    }

    /*
     * Returns the descent of the label.
     */
    public int getDescent() {
        return descent;
    }

}
