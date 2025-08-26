/*
 * Copyright (c) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.event;

import java.util.EventListener;

import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.common.board.Coords;

/**
 * Classes which implement this interface provide methods that deal with the events that are generated when the
 * BoardView is changed.
 * <p>
 * After creating an instance of a class that implements this interface it can be added to a Board using the
 * addBoardViewListener method and removed using the removeBoardViewListener method. When BoardView is changed the
 * appropriate method will be invoked.
 *
 * @see BoardViewListenerAdapter
 * @see BoardViewEvent
 */
public interface BoardViewListener extends EventListener {

    /**
     * Sent when user clicks, double clicks or drags hex.
     *
     * @param b an event
     */
    void hexMoused(BoardViewEvent b);

    /**
     * Sent when BoardView 'cursor' is set to Hex.
     *
     * @param b an event
     */
    void hexCursor(BoardViewEvent b);

    /**
     * Sent when Hex is highlighted.
     *
     * @param b an event
     */
    void boardHexHighlighted(BoardViewEvent b);

    /**
     * Sent when Hex is selected through a call to the select method (this is not a mouse click event, although many
     * mouse clicks lead to the hex being selected).
     *
     * @param b The select event
     *
     * @see IBoardView#select(Coords)
     */
    void hexSelected(BoardViewEvent b);

    /**
     * Sent when firstLOS is set.
     *
     * @param b an event
     */
    void firstLOSHex(BoardViewEvent b);

    /**
     * Sent when secondLOS is set.
     *
     * @param b an event
     */
    void secondLOSHex(BoardViewEvent b);

    /**
     * Sent when moving units is finished.
     *
     * @param b an event
     */
    void finishedMovingUnits(BoardViewEvent b);

    /**
     * Sent when Unit is selected.
     *
     * @param b an event
     */
    void unitSelected(BoardViewEvent b);
}
