/*
  Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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


package megamek.common;

/**
 * This interface represents Off-Board Directions
 */
public enum OffBoardDirection {

    NONE(-1),
    NORTH(0),
    SOUTH(1),
    EAST(2),
    WEST(3);

    private int value;

    OffBoardDirection(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static OffBoardDirection getDirection(int value) {
        for (OffBoardDirection dir : OffBoardDirection.values()) {
            if (dir.getValue() == value) {
                return dir;
            }
        }
        return NONE;
    }

    /**
     * Translate a "START_X" constant from Board.java into one of these values if possible.
     */
    public static OffBoardDirection translateBoardStart(int value) {
        switch (value) {
            case Board.START_N:
                return NORTH;
            case Board.START_S:
                return SOUTH;
            case Board.START_E:
                return EAST;
            case Board.START_W:
                return WEST;
            default:
                return NONE;
        }
    }

    /**
     * Gets the opposite direction of the given direction.
     */
    public static OffBoardDirection getOpposite(OffBoardDirection value) {
        switch (value) {
            case SOUTH:
                return NORTH;
            case NORTH:
                return SOUTH;
            case WEST:
                return EAST;
            case EAST:
                return WEST;
            default:
                return NONE;
        }
    }

    /**
     * Returns the OffBoardDirection associated with given the on-board deployment position as defined in
     * IStartingPositions.
     */
    public static OffBoardDirection translateStartPosition(int startPos) {
        if (startPos > 10) {
            startPos -= 10;
        }
        switch (startPos) {
            case 1:
            case 2:
            case 3:
                return OffBoardDirection.NORTH;
            case 4:
                return OffBoardDirection.EAST;
            case 5:
            case 6:
            case 7:
                return OffBoardDirection.SOUTH;
            case 8:
                return OffBoardDirection.WEST;
            default:
                return OffBoardDirection.NONE;
        }
    }
}
