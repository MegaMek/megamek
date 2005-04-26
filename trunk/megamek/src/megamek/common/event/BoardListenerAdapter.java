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


public class BoardListenerAdapter implements BoardListener {

    /* (non-Javadoc)
     * @see megamek.common.BoardListener#boardNewBoard(megamek.common.BoardEvent)
     */
    public void boardNewBoard(BoardEvent b) {
    }

    /* (non-Javadoc)
     * @see megamek.common.BoardListener#boardChangedHex(megamek.common.BoardEvent)
     */
    public void boardChangedHex(BoardEvent b) {
    }

}
