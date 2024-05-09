/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
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
    private static final long serialVersionUID = -4444092727458493689L;

    private final Board oldBoard;
    private final Board newBoard;
    private final int boardId;

    /**
     * Constructs the new event with the specified old/new board objects
     *
     * @param source The event source
     * @param oldBoard old game board
     * @param newBoard new game board
     */
    public GameBoardNewEvent(Object source, Board oldBoard, Board newBoard, int boardId) {
        super(source);
        this.oldBoard = oldBoard;
        this.newBoard = newBoard;
        this.boardId = boardId;
    }

    /**
     * @return Returns the newBoard.
     */
    public Board getNewBoard() {
        return newBoard;
    }

    /**
     * @return Returns the oldBoard.
     */
    public Board getOldBoard() {
        return oldBoard;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameBoardNew(this);
    }

    @Override
    public String getEventName() {
        return "New Board";
    }

    /** @return The ID of both the old and new Board. */
    public int getBoardId() {
        return boardId;
    }
}
