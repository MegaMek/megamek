/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.phaseDisplay;

import megamek.common.MovePath;

/**
 * This enum is used to indicate whether a landing is horizontal or vertical.
 */
public enum LandingDirection {
    VERTICAL, HORIZONTAL;

    /**
     * @return The MoveStepType that is used to indicate a landing of this direction in a move path.
     */
    public MovePath.MoveStepType moveStepType() {
        return this == VERTICAL ? MovePath.MoveStepType.VLAND : MovePath.MoveStepType.LAND;
    }

    public boolean isHorizontal() {
        return this == HORIZONTAL;
    }

    public boolean isVertical() {
        return this == VERTICAL;
    }
}
