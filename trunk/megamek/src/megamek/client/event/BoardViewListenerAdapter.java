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

public class BoardViewListenerAdapter implements BoardViewListener {

    /* (non-Javadoc)
     * @see megamek.client.BoardViewListener#boardHexMoused(megamek.client.BoardViewEvent)
     */
    public void boardHexMoused(BoardViewEvent b) {
    }

    /* (non-Javadoc)
     * @see megamek.client.BoardViewListener#boardHexCursor(megamek.client.BoardViewEvent)
     */
    public void boardHexCursor(BoardViewEvent b) {
    }

    /* (non-Javadoc)
     * @see megamek.client.BoardViewListener#boardHexHighlighted(megamek.client.BoardViewEvent)
     */
    public void boardHexHighlighted(BoardViewEvent b) {
    }

    /* (non-Javadoc)
     * @see megamek.client.BoardViewListener#boardHexSelected(megamek.client.BoardViewEvent)
     */
    public void boardHexSelected(BoardViewEvent b) {
    }

    /* (non-Javadoc)
     * @see megamek.client.BoardViewListener#boardFirstLOSHex(megamek.client.BoardViewEvent)
     */
    public void boardFirstLOSHex(BoardViewEvent b) {
    }

    /* (non-Javadoc)
     * @see megamek.client.BoardViewListener#boardSecondLOSHex(megamek.client.BoardViewEvent, megamek.common.Coords)
     */
    public void boardSecondLOSHex(BoardViewEvent b, Coords c) {
    }

    /* (non-Javadoc)
     * @see megamek.client.BoardViewListener#finishedMovingUnits(megamek.client.BoardViewEvent)
     */
    public void finishedMovingUnits(BoardViewEvent b) {
    }

    /* (non-Javadoc)
     * @see megamek.client.BoardViewListener#selectUnit(megamek.client.BoardViewEvent)
     */
    public void selectUnit(BoardViewEvent b) {
    }

}
