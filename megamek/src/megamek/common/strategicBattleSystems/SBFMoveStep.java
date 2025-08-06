/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.strategicBattleSystems;

import java.io.Serializable;

import megamek.common.BoardLocation;

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
     * Assembles and computes all data for this step. Tests if the start and end are on the same board and actually on
     * that board and if the step distance is more than one. Override to procide more tests.
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
