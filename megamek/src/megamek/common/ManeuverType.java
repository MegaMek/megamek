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

import megamek.common.MovePath.MoveStepType;

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
        if ((type >= MAN_NONE) && (type < MAN_SIZE)) {
            return names[type];
        }
        throw new IllegalArgumentException("Unknown maneuver type");
    }

    /**
     * determines whether the maneuver can be performed
     */
    public static boolean canPerform(int type, int velocity, int altitude, 
                                     int ceiling, boolean isVTOL, int distance, 
                                     Game game, MovePath mp) {

        // We can only perform one maneuver in a turn (important for side-slip)
        for (final MoveStep step : mp.getStepVector()) {
            if (step.getType() == MoveStepType.MANEUVER) {
                return false;
            }
        }
        
        // Side slip is the only maneuver that doesn't have to be at the start
        if ((distance > 0) && (type != MAN_SIDE_SLIP_LEFT)
                && (type != MAN_SIDE_SLIP_RIGHT)) {
            return false;
        }

        switch (type) {
            case MAN_NONE:
            case MAN_HAMMERHEAD:
            case MAN_HALF_ROLL:
                return true;
            case MAN_LOOP:
                return velocity >= 4;
            case MAN_IMMELMAN:
                return (velocity >= 3) && (altitude < 9);
            case MAN_SPLIT_S:
                return (altitude + 2) > ceiling;
            case MAN_BARREL_ROLL:
                return velocity >= 2;
            case MAN_SIDE_SLIP_LEFT:
            case MAN_SIDE_SLIP_RIGHT:
                if (velocity > 0) {
                    // If we're on a ground map, we need to make sure we can move
                    //  all 16 hexes
                    if (game.getBoard().getType() == Board.T_GROUND) {
                        MovePath tmpMp = mp.clone();                    
                        for (int i = 0; i < 8; i++) {
                            if (type == MAN_SIDE_SLIP_LEFT) {
                                tmpMp.addStep(MoveStepType.LATERAL_LEFT, true, true);
                            } else {
                                tmpMp.addStep(MoveStepType.LATERAL_RIGHT, true, true);
                            }
                        }
                        for (int i = 0; i < 8; i++) {
                            tmpMp.addStep(MoveStepType.FORWARDS, true, true);
                        }                    
                        return tmpMp.getLastStep().isLegal(tmpMp);
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            case MAN_VIFF:
                return isVTOL;
            default:
                return false;
        }
    }

    /**
     * Thrust cost of maneuver
     */
    public static int getCost(int type, int velocity) {
        switch (type) {
            case MAN_LOOP:
            case MAN_IMMELMAN:
                return 4;
            case MAN_SPLIT_S:
                return 2;
            case MAN_HAMMERHEAD:
                return velocity;
            case MAN_HALF_ROLL:
            case MAN_BARREL_ROLL:
            case MAN_SIDE_SLIP_LEFT:
            case MAN_SIDE_SLIP_RIGHT:
                return 1;
            case MAN_VIFF:
                return velocity + 2;
            default:
                return 0;
        }
    }

    /**
     * Returns the Control Roll modifier for a particular maneuver.  
     * 
     * @param type       The type of maneuver performed
     * @param isVSTOLCF  Flag that determines whether the maneuvering unit is 
     *                   a conventional fighter with VSTOl, which has effects
     *                   for side-slips
     *                   
     * @return The control roll modifier
     */
    public static int getMod(int type, boolean isVSTOLCF) {
        switch (type) {
            case MAN_LOOP:
            case MAN_IMMELMAN:
                return 1;
            case MAN_SPLIT_S:
            case MAN_VIFF:
                return 2;
            case MAN_HAMMERHEAD:
                return 3;
            case MAN_HALF_ROLL:
                return -1;
            case MAN_SIDE_SLIP_LEFT:
            case MAN_SIDE_SLIP_RIGHT:
                return isVSTOLCF ? -1 : 0;
            case MAN_BARREL_ROLL:
            default:
                return 0;
        }
    }
}
