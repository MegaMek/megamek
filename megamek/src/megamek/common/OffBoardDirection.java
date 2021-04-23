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
        switch(value) {
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
        switch(value) {
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
     * Returns the OffBoardDirection associated with given the on-board deployment
     * position as defined in IStartingPositions.
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
