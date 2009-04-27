/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 * Contains the to-hit number and a short description of how it was reached
 */
public class ToHitData extends TargetRoll {

    private static final long serialVersionUID = 737321999301910678L;
    public static final int HIT_NORMAL = 0;
    public static final int HIT_PUNCH = 1;
    public static final int HIT_KICK = 2;
    public static final int HIT_SWARM = 3;
    public static final int HIT_ABOVE = 4;
    public static final int HIT_BELOW = 5;
    public static final int HIT_PARTIAL_COVER = 6;
    public static final int HIT_SWARM_CONVENTIONAL = 7;
    public static final int HIT_UNDERWATER = 8;

    public static final int SIDE_FRONT = 0;
    public static final int SIDE_REAR = 1;
    public static final int SIDE_LEFT = 2;
    public static final int SIDE_RIGHT = 3;
    public static final int SIDE_FRONTLEFT = 4;
    public static final int SIDE_FRONTRIGHT = 5;
    public static final int SIDE_REARLEFT = 6;
    public static final int SIDE_REARRIGHT = 7;
    public static final int SIDE_RANDOM = 8;

    private int hitTable = HIT_NORMAL;
    private int sideTable = SIDE_FRONT;
    private int cover = LosEffects.COVER_NONE;
    private int margineOfSuccess = 0;

    /**
     * Construct default.
     */
    public ToHitData() {
        super();
    }

    /**
     * Construct with value and desc. Other values default.
     */
    public ToHitData(int value, String desc) {
        this(value, desc, HIT_NORMAL, SIDE_FRONT);
    }

    /**
     * Construct with all variables.
     */
    public ToHitData(int value, String desc, int hitTable, int sideTable) {
        super(value, desc);
        this.hitTable = hitTable;
        this.sideTable = sideTable;
    }

    public int getHitTable() {
        return hitTable;
    }

    public void setHitTable(int hitTable) {
        this.hitTable = hitTable;
    }

    /**
     * Get the side being targeted. If the targeted side is determined randomly,
     * the calculation occurs each time the side is requested.
     *
     * @return an <code>int</code> that represents the side being targeted;
     *         the value will be one of SIDE_FRONT, SIDE_REAR, SIDE_LEFT, or
     *         SIDE_RIGHT, and *never* SIDE_RANDOM.
     */
    public int getSideTable() {
        int side = sideTable;
        if (side == SIDE_RANDOM) {
            side = Compute.randomInt(4);
        }
        return side;
    }

    public void setSideTable(int sideTable) {
        this.sideTable = sideTable;
    }

    /**
     * Describes the table and side we'return hitting on
     */
    public String getTableDesc() {
        if ((sideTable != SIDE_FRONT) || (hitTable != HIT_NORMAL)) {
            String tdesc = new String();
            switch (sideTable) {
                case SIDE_RANDOM:
                    tdesc += "Random Side ";
                    break;
                case SIDE_RIGHT:
                    tdesc += "Right Side ";
                    break;
                case SIDE_LEFT:
                    tdesc += "Left Side ";
                    break;
                case SIDE_REAR:
                    tdesc += "Rear ";
                    break;
            }
            switch (hitTable) {
                case HIT_PUNCH:
                    tdesc += "Punch ";
                    break;
                case HIT_KICK:
                    tdesc += "Kick ";
                    break;
                case HIT_SWARM:
                case HIT_SWARM_CONVENTIONAL:
                    tdesc += "Swarm ";
                    break;
                case HIT_ABOVE:
                    tdesc += "Above ";
                    break;
                case HIT_BELOW:
                    tdesc += "Below ";
                    break;
                case HIT_PARTIAL_COVER:
                    tdesc += "Partial cover ";
                    break;
            }
            return " (using " + tdesc + "table)";
        }
        return "";
    }

    public int getCover() {
        return cover;
    }

    public void setCover(int cover) {
        this.cover = cover;
    }

    public int getMoS() {
        return margineOfSuccess;
    }

    public void setMoS(int moS) {
        margineOfSuccess = moS;
    }

}
