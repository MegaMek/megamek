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
    public static final int HIT_SPECIAL_PROTO = 9;
    public static final int HIT_SPHEROID_CRASH = 10;

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

    private Coords location;

    private int range;

    /**
     * Indicates if the primary cover is damagable.
     */
    int damagableCoverTypePrimary = LosEffects.DAMAGABLE_COVER_NONE;
    /**
     * Indicates if the secondary cover is damagable
     */
    int damagableCoverTypeSecondary = LosEffects.DAMAGABLE_COVER_NONE;
    /**
     * Keeps track of the building that provides cover.  This is used
     * to assign damage for shots that hit cover.  The primary cover is used
     * if there is a sole piece of cover (horizontal cover, 25% cover).
     * In the case of a primary and secondary, the primary cover protects the
     * right side.
     */
    Building coverBuildingPrimary = null;
    /**
     * Keeps track of the building that provides cover.  This is used
     * to assign damage for shots that hit cover.  The secondary cover is used
     * if there are two buildings that provide cover, like in the case of 75%
     * cover or two buildings providing 25% cover for a total of horizontal
     * cover.  The secondary cover protects the left side.
     */
    Building coverBuildingSecondary = null;
    /**
     * Keeps track of the grounded Dropship that provides cover.  This is
     * used to assign damage for shots that hit cover. The primary cover is used
     * if there is a sole piece of cover (horizontal cover, 25% cover).
     * In the case of a primary and secondary, the primary cover protects the
     * right side.
     */
    Entity coverDropshipPrimary = null;
    /**
     * Keeps track of the grounded Dropship that provides cover.  This is
     * used to assign damage for shots that hit cover. The secondary cover is used
     * if there are two buildings that provide cover, like in the case of 75%
     * cover or two buildings providing 25% cover for a total of horizontal
     * cover.  The secondary cover protects the left side.
     */
    Entity coverDropshipSecondary = null;
    /**
     * Stores the hex location of the primary cover.
     */
    Coords coverLocPrimary = null;
    /**
     * Stores the hex location of the secondary cover.
     */
    Coords coverLocSecondary = null;

    /**
     * Construct default.
     */
    public ToHitData() {
        super();
    }

    /**
     * Construct with a target roll modifier right off the bat.
     *
     * @param targetRollModifier The {@link TargetRollModifier} that applies immediately.
     */
    public ToHitData(TargetRollModifier targetRollModifier) {
        this(targetRollModifier.getValue(), targetRollModifier.getDesc());
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
                    tdesc += "Partial cover " +
                            "(" + LosEffects.getCoverName(cover, true) + ") ";
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

    /**
     * Determines whether the Margin of success or failure
     * for a given roll.
     * MoS returns a positive while
     * MoF returns a negative
     *
     * @return <code>int</code> 
     */
    public int getMoS() {
        return margineOfSuccess;
    }

    public void setMoS(int moS) {
        margineOfSuccess = moS;
    }

    public void setLocation(Coords l) {
        location = l;
    }

    public Coords getLocation() {
        return location;
    }

    public void setRange(int r) {
        range = r;
    }

    public int getRange() {
        return range;
    }

    public void setDamagableCoverTypePrimary(int damagableCoverType) {
        damagableCoverTypePrimary = damagableCoverType;
    }

    public int getDamagableCoverTypePrimary() {
        return damagableCoverTypePrimary;
    }

    public Entity getCoverDropshipPrimary() {
        return coverDropshipPrimary;
    }

    public void setCoverDropshipPrimary(Entity coverDropship) {
        coverDropshipPrimary = coverDropship;
    }

    public Building getCoverBuildingPrimary() {
        return coverBuildingPrimary;
    }

    public void setCoverBuildingPrimary(Building coverBuilding) {
        coverBuildingPrimary = coverBuilding;
    }

    public Coords getCoverLocPrimary() {
        return coverLocPrimary;
    }

    public void setCoverLocPrimary(Coords coverLoc) {
        coverLocPrimary = coverLoc;
    }

    public void setDamagableCoverTypeSecondary(int damagableCoverType) {
        damagableCoverTypeSecondary = damagableCoverType;
    }

    public int getDamagableCoverTypeSecondary() {
        return damagableCoverTypeSecondary;
    }

    public Entity getCoverDropshipSecondary() {
        return coverDropshipSecondary;
    }

    public void setCoverDropshipSecondary(Entity coverDropship) {
        coverDropshipSecondary = coverDropship;
    }

    public Building getCoverBuildingSecondary() {
        return coverBuildingSecondary;
    }

    public void setCoverBuildingSecondary(Building coverBuilding) {
        coverBuildingSecondary = coverBuilding;
    }

    public Coords getCoverLocSecondary() {
        return coverLocSecondary;
    }

    public void setCoverLocSecondary(Coords coverLoc) {
        coverLocSecondary = coverLoc;
    }

}
