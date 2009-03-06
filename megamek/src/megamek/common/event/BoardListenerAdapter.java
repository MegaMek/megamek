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
 * This adapter class provides default implementations for the methods described
 * by the <code>BoardListener</code> interface.
 * <p>
 * Classes that wish to deal with <code>BoardEvent</code>s can extend this
 * class and override only the methods which they are interested in.
 * </p>
 *
 * @see BoardListener
 * @see BoardEvent
 */
public class BoardListenerAdapter implements BoardListener {

    /**
     * Sent when Board completely changed The default behavior is to do nothing.
     *
     * @param b an event containing information about the change
     */
    public void boardNewBoard(BoardEvent b) {
    }

    /**
     * Sent when Hex on the Board changed The default behavior is to do nothing.
     *
     * @param b an event containing information about the change
     */
    public void boardChangedHex(BoardEvent b) {
    }

    /**
     * Sent when all Hexes on the Board changed The default behavior is to do nothing.
     *
     * @param b an event containing information about the change
     */
    public void boardChangedAllHexes(BoardEvent b) {
    }

}
