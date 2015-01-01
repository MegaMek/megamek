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

package megamek.common.event;

/**
 * Classes which implement this interface provide methods that deal with the
 * events that are generated when the Board is changed.
 * <p>
 * After creating an instance of a class that implements this interface it can
 * be added to a Board using the <code>addBoardListener</code> method and
 * removed using the <code>removeBoardListener</code> method. When board is
 * changed the appropriate method will be invoked.
 * </p>
 *
 * @see BoardListenerAdapter
 * @see BoardEvent
 */
public interface BoardListener extends java.util.EventListener {
    /**
     * Sent when Board completely changed
     *
     * @param b an event containing information about the change
     */
    public void boardNewBoard(BoardEvent b);

    /**
     * Sent when Hex on the Board changed
     *
     * @param b an event containing information about the change
     */
    public void boardChangedHex(BoardEvent b);

    /**
     * Sent when all hexes on the board changed
     *
     * @param b an event containing information about the change
     */
    public void boardChangedAllHexes(BoardEvent b);

}
