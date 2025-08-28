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
package megamek.common.board;

/**
 * Represents a set of cube coordinates in a hex grid. Cube Coordinate allows for more precise manipulation of
 * distances, movements, and other operations.
 *
 * @author Luana Coppio
 */
public enum CardinalDirection {
    NORTH(new CubeCoords(0, -1, 1), 0),
    NORTHEAST(new CubeCoords(1, -1, 0), 60),
    SOUTHEAST(new CubeCoords(1, 0, -1), 120),
    SOUTH(new CubeCoords(0, 1, -1), 180),
    SOUTHWEST(new CubeCoords(-1, 1, 0), 240),
    NORTHWEST(new CubeCoords(-1, 0, 1), 300);

    private final CubeCoords direction;
    private final int angle;

    CardinalDirection(CubeCoords direction, int angle) {
        this.direction = direction;
        this.angle = angle;
    }

    /**
     * Returns the direction of this CardinalDirection as CubeCoords.
     *
     * @return the CubeCoords representing the direction
     */
    public CubeCoords getDirection() {
        return direction;
    }

    /**
     * Returns the angle of this CardinalDirection in degrees.
     *
     * @return the angle in degrees
     */
    public int getAngle() {
        return angle;
    }
}
