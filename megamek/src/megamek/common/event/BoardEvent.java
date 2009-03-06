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

import megamek.common.Coords;

/**
 * Instances of this class are sent as a result of Board change
 *
 * @see BoardListener
 */
public class BoardEvent extends java.util.EventObject {
    /**
     *
     */
    private static final long serialVersionUID = 6895134212472497607L;
    public static final int BOARD_NEW_BOARD = 0;
    public static final int BOARD_CHANGED_HEX = 1;
    public static final int BOARD_CHANGED_ALL_HEXES = 2;

    private Coords coords;
    private int type;

    public BoardEvent(Object source, Coords coords, int type) {
        super(source);
        this.coords = coords;
        this.type = type;
    }

    /**
     * @return the type of event that this is
     */
    public int getType() {
        return type;
    }

    /**
     * @return the coordinate where this event occurred, if applicable;
     *         <code>null</code> otherwise.
     */
    public Coords getCoords() {
        return coords;
    }
}
