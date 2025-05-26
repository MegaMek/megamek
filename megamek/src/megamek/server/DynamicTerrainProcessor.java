/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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
package megamek.server;

import java.util.Collection;
import java.util.Vector;

import megamek.common.Board;
import megamek.common.BoardLocation;
import megamek.common.Coords;
import megamek.common.Report;
import megamek.server.totalwarfare.TWGameManager;

public abstract class DynamicTerrainProcessor {
    protected TWGameManager gameManager;

    DynamicTerrainProcessor(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Process terrain changes in the end phase
     *
     * @param vPhaseReport reports for the server to send out
     */
    public abstract void doEndPhaseChanges(Vector<Report> vPhaseReport);

    /**
     * Marks the given hex as changed. All changes are sent to the clients after terrain processing is completed.
     *
     * @param coords  The coords of the hex
     * @param boardId The board ID of the hex
     */
    void markHexUpdate(Coords coords, int boardId) {
        gameManager.getHexUpdateSet().add(BoardLocation.of(coords, boardId));
    }

    /**
     * Marks the given hex on the given Board as changed. All changes are sent to the clients after terrain processing
     * is completed.
     *
     * @param coords The coords of the hex
     * @param board  The board of the hex
     */
    void markHexUpdate(Coords coords, Board board) {
        markHexUpdate(coords, board.getBoardId());
    }

    /**
     * Marks the given hexes as changed. All changes are sent to the clients after terrain processing is completed.
     *
     * @param coords  The coords of the hexes
     * @param boardId The board ID of all hexes
     */
    void markHexUpdate(Collection<Coords> coords, int boardId) {
        coords.forEach(c -> markHexUpdate(c, boardId));
    }
}
