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
 * Classes which implement this interface provide methods that deal with the
 * events that are generated when the BoardView is changed.
 * <p>
 * After creating an instance of a class that implements this interface it can
 * be added to a Board using the <code>addBoardViewListener</code> method and
 * removed using the <code>removeBoardViewListener</code> method. When
 * BoardView is changed the appropriate method will be invoked.
 * </p>
 * 
 * @see BoardViewListenerAdapter
 * @see BoardViewEvent
 */
public interface BoardViewListener extends java.util.EventListener {

    /**
     * Sent when user clicks, double clicks or drags hex.
     * 
     * @param b an event
     */
    public void hexMoused(BoardViewEvent b);

    /**
     * Sent when BoardView 'cursor' is set to Hex.
     * 
     * @param b an event
     */
    public void hexCursor(BoardViewEvent b);

    /**
     * Sent when Hex is highlighted.
     * 
     * @param b an event
     */
    public void boardHexHighlighted(BoardViewEvent b);

    /**
     * Sent when Hex is selected.
     * 
     * @param b an event
     */
    public void hexSelected(BoardViewEvent b);

    /**
     * Sent when firstLOS is set.
     * 
     * @param b an event
     */
    public void firstLOSHex(BoardViewEvent b);

    /**
     * Sent when secondLOS is set.
     * 
     * @param b an event
     */
    public void secondLOSHex(BoardViewEvent b, Coords c);

    /**
     * Sent when moving units is finished.
     * 
     * @param b an event
     */
    public void finishedMovingUnits(BoardViewEvent b);

    /**
     * Sent when Unit is selected.
     * 
     * @param b an event
     */
    public void unitSelected(BoardViewEvent b);
}
