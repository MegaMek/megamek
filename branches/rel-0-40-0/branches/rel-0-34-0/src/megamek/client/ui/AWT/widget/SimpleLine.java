/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;

public class SimpleLine extends Canvas {

    /**
     * 
     */
    private static final long serialVersionUID = 8615969226702396249L;
    private int h = 2;
    private int w;

    public SimpleLine(int width) {
        w = width;
        setSize(w, h);
    }

    public void paint(Graphics g) {
        g.setColor(Color.gray);
        g.drawLine(0, 0, w, 0);
        g.setColor(Color.white);
        g.drawLine(0, 1, w, 1);
    }

}
