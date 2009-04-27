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
 * Maneuver types for Aeros
 */
public class ManeuverType {
    
    public static final int MAN_NONE = 0;
    public static final int MAN_LOOP = 1;
    public static final int MAN_IMMELMAN = 2;
    public static final int MAN_SPLIT_S = 3;
    public static final int MAN_HAMMERHEAD = 4;
    public static final int MAN_HALF_ROLL = 5;
    public static final int MAN_BARREL_ROLL = 6;
    public static final int MAN_SIDE_SLIP_LEFT = 7;
    public static final int MAN_SIDE_SLIP_RIGHT = 8;
    public static final int MAN_VIFF = 9;
    
    private static String[] names = { "None", "Loop", "Immelman", "Split S",
                                      "Hammerhead", "Half Roll", "Barrel Roll", "Side Slip (Left)",
                                      "Side Slip (Right)", "VIFF"};
  
    public static final int MAN_SIZE = names.length;
    
    public static String getTypeName(int type) {
        if (type >= MAN_NONE && type < MAN_SIZE) {
            return names[type];
        }
        throw new IllegalArgumentException("Unknown maneuver type");
    }
    
    /*
     * determines whether the maneuver can be performed
     */
    public static boolean canPerform(int type, int velocity, int altitude, int ceiling, 
                                     boolean isVTOL, int distance) {
        
        //if the Aero has moved to any hexes, then it can no longer perform 
        //any maneuver except side slip
        if(distance > 0 && type != MAN_SIDE_SLIP_LEFT && type != MAN_SIDE_SLIP_RIGHT) {
            return false;
        }
        
        switch(type) {
        case (MAN_NONE):
            return true;
        case (MAN_LOOP):
            if(velocity >= 4)
                return true;
            else 
                return false;
        case (MAN_IMMELMAN):
            if(velocity >= 3 && altitude < 9) 
                return true;
            else
                return false;
        case (MAN_SPLIT_S):
            if((altitude + 2) > ceiling) 
                return true;
            else
                return false;
        case (MAN_HAMMERHEAD):
            return true;
        case (MAN_HALF_ROLL):
            return true;
        case (MAN_BARREL_ROLL):
            if(velocity >= 2)
                return true;
            else 
                return false;
        case (MAN_SIDE_SLIP_LEFT):
        case (MAN_SIDE_SLIP_RIGHT):
            if(velocity > 0)
                return true;
            else 
                return false;
        case (MAN_VIFF):
            if(isVTOL)
                return true;
            else
                return false;
        default:
            return false;
        }
    }
    
    /*
     * thrust cost of maneuver
     */
    public static int getCost(int type, int velocity) {
        switch(type) {
        case (MAN_LOOP):
            return 4;
        case (MAN_IMMELMAN):
            return 4;
        case (MAN_SPLIT_S):
            return 2;
        case (MAN_HAMMERHEAD):
            return velocity;
        case (MAN_HALF_ROLL):
            return 1;
        case (MAN_BARREL_ROLL):
            return 1;
        case (MAN_SIDE_SLIP_LEFT):
        case (MAN_SIDE_SLIP_RIGHT):
            return 1;
        case (MAN_VIFF):
            return velocity + 2;
        default:
            return 0;
        }
    }
    
    /*
     * Control roll modifier
     */
    public static int getMod(int type, boolean isVTOL) {
        switch(type) {
        case (MAN_LOOP):
            return 1;
        case (MAN_IMMELMAN):
            return 1;
        case (MAN_SPLIT_S):
            return 2;
        case (MAN_HAMMERHEAD):
            return 3;
        case (MAN_HALF_ROLL):
            return -1;
        case (MAN_BARREL_ROLL):
            return 0;
        case (MAN_SIDE_SLIP_LEFT):
        case (MAN_SIDE_SLIP_RIGHT):
            if(isVTOL)
                return -1;
            else
                return 0;
        case (MAN_VIFF):
            return 2;
        default:
            return 0;
        }
    }
    
}