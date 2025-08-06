/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.event;

import megamek.common.Board;
import megamek.common.Game;

/**
 * Instances of this class are sent when the new board for game is set
 *
 * @see Game#setBoard(Board)
 * @see GameListener
 */
public class GameBoardNewEvent extends GameEvent {

    private final Board oldBoard;
    private final Board newBoard;
    private final int boardId;

    /**
     * Constructs the new event with the specified old/new board objects
     *
     * @param source   The event source
     * @param oldBoard old game board
     * @param newBoard new game board
     */
    public GameBoardNewEvent(Object source, Board oldBoard, Board newBoard, int boardId) {
        super(source);
        this.oldBoard = oldBoard;
        this.newBoard = newBoard;
        this.boardId = boardId;
    }

    public Board getNewBoard() {
        return newBoard;
    }

    public Board getOldBoard() {
        return oldBoard;
    }

    @Override
    public void fireEvent(GameListener gameListener) {
        gameListener.gameBoardNew(this);
    }

    @Override
    public String getEventName() {
        return "New Board";
    }

    /**
     * @return The ID of both the old and new Board.
     */
    public int getBoardId() {
        return boardId;
    }
}
