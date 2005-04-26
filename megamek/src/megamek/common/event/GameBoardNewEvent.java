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

import megamek.common.IBoard;

public class GameBoardNewEvent extends GameEvent {

    protected IBoard oldBoard;
    protected IBoard newBoard;
    
    /**
     * Constructs the new event with the specified old/new board objects 
     * @param source The event source
     * @param oldBoard old game board
     * @param newBoard new game oard
     */
    public GameBoardNewEvent(Object source, IBoard oldBoard, IBoard newBoard) {
        super(source, GAME_BOARD_NEW);
        this.oldBoard = oldBoard;
        this.newBoard = newBoard;
    }

    /**
     * @return Returns the newBoard.
     */
    public IBoard getNewBoard() {
        return newBoard;
    }
    /**
     * @return Returns the oldBoard.
     */
    public IBoard getOldBoard() {
        return oldBoard;
    }

}
