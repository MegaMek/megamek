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

public class SBFMoveStep implements Serializable {

    BoardLocation startingPoint;
    BoardLocation destination;

    private SBFMoveStep() { }

    public static SBFMoveStep createGroundMovementStep(BoardLocation startingPoint, BoardLocation destination) {
        SBFMoveStep step = new SBFMoveStep();
        step.destination = destination;
        return step;
    }

    public static SBFMoveStep createGroundMovementStep(BoardLocation startingPoint, int direction) {
        SBFMoveStep step = new SBFMoveStep();
        step.startingPoint = startingPoint;
        step.destination = startingPoint.translated(direction);
        return step;
    }

    public static SBFMoveStep createStep(SBFMoveStep original) {
        return new SBFMoveStep();
    }

    public BoardLocation getLastPosition() {
        return destination;
    }

}
