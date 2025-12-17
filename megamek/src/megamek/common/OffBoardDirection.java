/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.board.Board;

/**
 * This interface represents Off-Board Directions
 */
public enum OffBoardDirection {

    NONE(-1),
    NORTH(0),
    SOUTH(1),
    EAST(2),
    WEST(3);

    private final int value;

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
        return switch (value) {
            case Board.START_N -> NORTH;
            case Board.START_S -> SOUTH;
            case Board.START_E -> EAST;
            case Board.START_W -> WEST;
            default -> NONE;
        };
    }

    /**
     * Gets the opposite direction of the given direction.
     */
    public static OffBoardDirection getOpposite(OffBoardDirection value) {
        return switch (value) {
            case SOUTH -> NORTH;
            case NORTH -> SOUTH;
            case WEST -> EAST;
            case EAST -> WEST;
            default -> NONE;
        };
    }

    /**
     * Converts this OffBoardDirection to a Board.START_X constant.
     *
     * @return The corresponding Board.START_X value, or Board.START_ANY for NONE
     */
    public int translateBoardStart() {
        return switch (this) {
            case NORTH -> Board.START_N;
            case SOUTH -> Board.START_S;
            case EAST -> Board.START_E;
            case WEST -> Board.START_W;
            default -> Board.START_ANY;
        };
    }

    /**
     * Creates an OffBoardDirection from a Board.START_X constant. This is the inverse of translateBoardStart().
     *
     * @param boardStart A Board.START_X constant
     *
     * @return The corresponding OffBoardDirection, or NONE if not recognized
     */
    public static OffBoardDirection fromBoardStart(int boardStart) {
        return translateBoardStart(boardStart);
    }

    /**
     * Returns the OffBoardDirection associated with given the on-board deployment position as defined in
     * IStartingPositions.
     */
    public static OffBoardDirection translateStartPosition(int startPos) {
        if (startPos > 10) {
            startPos -= 10;
        }
        return switch (startPos) {
            case 1, 2, 3 -> OffBoardDirection.NORTH;
            case 4 -> OffBoardDirection.EAST;
            case 5, 6, 7 -> OffBoardDirection.SOUTH;
            case 8 -> OffBoardDirection.WEST;
            default -> OffBoardDirection.NONE;
        };
    }
}
