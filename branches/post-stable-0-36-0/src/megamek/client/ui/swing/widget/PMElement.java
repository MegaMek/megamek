/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.swing.widget;

/**
 * Generic element of PicMap component
 */

import java.awt.Graphics;
import java.awt.Rectangle;

public interface PMElement {
    /**
     * Translates element by x and y.
     */
    public void translate(int x, int y);

    /**
     * Sets visibility of element
     */
    public void setVisible(boolean v);

    /**
     * Draws element into specifyed Graphics.
     */
    public void drawInto(Graphics g);

    /**
     * Return bounding box of element.
     */
    public Rectangle getBounds();
}