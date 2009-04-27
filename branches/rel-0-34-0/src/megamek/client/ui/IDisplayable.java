/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
 *
 * This file (C) 2008 Jörg Walter <j.walter@syntax-k.de>
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

package megamek.client.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

/**
 *
 * An object that is displayed over the part of the IBoardView
 * that is currently visible
 * @author jwalt
 */
public interface IDisplayable {

    public boolean isBeingDragged();

    public boolean isDragged(Point point, Dimension backSize);

    public boolean isHit(Point point, Dimension size);

    public boolean isMouseOver(Point point, Dimension backSize);

    public boolean isReleased();

    /**
     * Draw this IDisplayable
     * @param graph -           the <code>Graphics</code> to draw on
     * @param drawRelativeTo -  the top left <code>Point</code>
     *                          of the viewport, relative to the Graphics
     *                          this needs to be drawn on
     * @param size -            the size of the viewport in
     *                          which this should be draw
     */
    public void draw(Graphics graph, Point drawRelativeTo, Dimension size);

    public boolean isSliding();

    public void setIdleTime(long l, boolean b);

    public boolean slide();

}
