/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.client.event;

import megamek.common.Coords;

/**
 * Classes which implement this interface provide methods
 * that deal with the events that are generated when the
 * BoardView is changed.
 * <p>
 * After creating an instance of a class that implements
 * this interface it can be added to a Board using the
 * <code>addBoardViewListener</code> method and removed using
 * the <code>removeBoardViewListener</code> method. 
 * When BoardView is changed the appropriate method will be invoked.
 * </p>
 *
 * @see BoardViewListenerAdapter
 * @see BoardViewEvent
 */
public interface BoardViewListener extends java.util.EventListener
{
    public void boardHexMoused(BoardViewEvent b);

    public void boardHexCursor(BoardViewEvent b);
    public void boardHexHighlighted(BoardViewEvent b);
    public void boardHexSelected(BoardViewEvent b);

    public void boardFirstLOSHex(BoardViewEvent b);
    public void boardSecondLOSHex(BoardViewEvent b, Coords c);

    public void finishedMovingUnits(BoardViewEvent b);
    public void selectUnit(BoardViewEvent b);
}
