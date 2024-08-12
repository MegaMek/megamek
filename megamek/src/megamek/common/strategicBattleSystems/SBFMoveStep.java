/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.strategicBattleSystems;

import megamek.common.BoardLocation;

import java.io.Serializable;

public abstract class SBFMoveStep implements Serializable {

    protected final int formationId;
    protected BoardLocation startingPoint;
    protected BoardLocation destination;
    protected int mpUsed = 1;
    protected boolean isIllegal = false;

    protected SBFMoveStep(int formationId) {
        this.formationId = formationId;
    }

    /**
     * Assembles and computes all data for this step. Tests if the start and end are on the same board and
     * actually on that board and if the step distance is more than one. Override to procide more tests.
     *
     * @param game The game
     */
    protected void compile(SBFGame game) {
        if (!game.hasBoardLocation(startingPoint) || !game.hasBoardLocation(destination)
                || (startingPoint.boardId() != destination.boardId())
                || (startingPoint.coords().distance(destination.coords()) > 1)) {
            isIllegal = true;
        }
    }

    /**
     * @return An exact copy of this move step.
     */
    public abstract SBFMoveStep copy();

    public BoardLocation getLastPosition() {
        return destination;
    }

    public int getMpUsed() {
        return mpUsed;
    }

    public BoardLocation getDestination() {
        return destination;
    }

    public boolean isIllegal() {
        return isIllegal;
    }

    public int getMovementDirection() {
        return -1;
    }
}
