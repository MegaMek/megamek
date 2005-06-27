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

package megamek.client.ui.AWT;

import java.awt.*;

public interface Displayable {
    
    public void draw(Graphics graph, Dimension size);
    public void setIdleTime(long timeIdle, boolean add);
    public boolean isHit(Point p, Dimension size);
    public boolean isMouseOver(Point p, Dimension size);
    public boolean isDragged(Point p, Dimension size);
    public boolean isBeingDragged();
    public boolean isSliding();
    public boolean slide();
    public boolean isReleased();
}
