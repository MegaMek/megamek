/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General License for more details.
 *
 * You should have received a copy of the GNU General License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.event;

import megamek.common.Coords;
import megamek.client.ui.swing.boardview.IBoardView;

import java.util.EventListener;

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
