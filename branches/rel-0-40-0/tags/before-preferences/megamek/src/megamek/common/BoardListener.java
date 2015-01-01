/**
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

package megamek.common;

public interface BoardListener
    extends java.util.EventListener
{
    public void boardHexMoused(BoardEvent b);

    public void boardHexCursor(BoardEvent b);
    public void boardHexHighlighted(BoardEvent b);
    public void boardHexSelected(BoardEvent b);

    public void boardNewBoard(BoardEvent b);
    
    public void boardChangedHex(BoardEvent b);
    public void boardFirstLOSHex(BoardEvent b);
    public void boardSecondLOSHex(BoardEvent b, Coords c);
    public void boardChangedEntity(BoardEvent b);
    public void boardNewAttack(BoardEvent a);
}
