/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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


import java.io.Serial;
import java.util.List;

import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;

/**
 * Contains the to-hit number and a short description of how it was reached
 */
public class ToHitData extends TargetRoll {

    @Serial
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
    public static final int SIDE_FRONT_LEFT = 4;
    public static final int SIDE_FRONT_RIGHT = 5;
    public static final int SIDE_REAR_LEFT = 6;
    public static final int SIDE_REAR_RIGHT = 7;
    public static final int SIDE_RANDOM = 8;

    private int hitTable = HIT_NORMAL;
    private int sideTable = SIDE_FRONT;
    private int cover = LosEffects.COVER_NONE;
    private int marginOfSuccess = 0;

    private Coords location;

    private int range;

    /**
     * Tracks Enhanced Imaging (EI) terrain reduction. EI reduces woods/smoke modifiers by 1 per hex (minimum +1 per
     * hex). This accumulates the total reduction to be added as a single modifier at the end via
     * {@link #finalizeEiModifier()}.
     */
    private int eiReduction = 0;

    /**
     * Indicates if the primary cover is damagable.
     */
    int damagableCoverTypePrimary = LosEffects.DAMAGABLE_COVER_NONE;
    /**
     * Indicates if the secondary cover is damagable
     */
    int damagableCoverTypeSecondary = LosEffects.DAMAGABLE_COVER_NONE;
    /**
     * Keeps track of the building that provides cover.  This is used to assign damage for shots that hit cover.  The
     * primary cover is used if there is a sole piece of cover (horizontal cover, 25% cover). In the case of a primary
     * and secondary, the primary cover protects the right side.
     */
    IBuilding coverBuildingPrimary = null;
    /**
     * Keeps track of the building that provides cover.  This is used to assign damage for shots that hit cover.  The
     * secondary cover is used if there are two buildings that provide cover, like in the case of 75% cover or two
     * buildings providing 25% cover for a total of horizontal cover.  The secondary cover protects the left side.
     */
    IBuilding coverBuildingSecondary = null;
    /**
     * Keeps track of the grounded Dropship that provides cover.  This is used to assign damage for shots that hit
     * cover. The primary cover is used if there is a sole piece of cover (horizontal cover, 25% cover). In the case of
     * a primary and secondary, the primary cover protects the right side.
     */
    Entity coverDropshipPrimary = null;
    /**
     * Keeps track of the grounded Dropship that provides cover.  This is used to assign damage for shots that hit
     * cover. The secondary cover is used if there are two buildings that provide cover, like in the case of 75% cover
     * or two buildings providing 25% cover for a total of horizontal cover.  The secondary cover protects the left
     * side.
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
     * Keeps track of the <code>LosEffects</code> thruBldg value, which tracks if combat within a building is happening.
     * That is, if LoS from the attacker to target is traced  through a single building, then this value will be
     * non-null.
     */
    IBuilding thruBldg = null;

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
        this(targetRollModifier.value(), targetRollModifier.getDesc());
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
     * Get the side being targeted. If the targeted side is determined randomly, the calculation occurs each time the
     * side is requested.
     *
     * @return an <code>int</code> that represents the side being targeted; the value will be one of SIDE_FRONT,
     *       SIDE_REAR, SIDE_LEFT, or SIDE_RIGHT, and *never* SIDE_RANDOM.
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
     * Describes the table and side we're turn hitting on
     */
    public String getTableDesc() {
        if ((sideTable != SIDE_FRONT) || (hitTable != HIT_NORMAL)) {
            String tableDesc = "";
            switch (sideTable) {
                case SIDE_RANDOM:
                    tableDesc += "Random Side ";
                    break;
                case SIDE_RIGHT:
                    tableDesc += "Right Side ";
                    break;
                case SIDE_LEFT:
                    tableDesc += "Left Side ";
                    break;
                case SIDE_REAR:
                    tableDesc += "Rear ";
                    break;
            }
            switch (hitTable) {
                case HIT_PUNCH:
                    tableDesc += "Punch ";
                    break;
                case HIT_KICK:
                    tableDesc += "Kick ";
                    break;
                case HIT_SWARM:
                case HIT_SWARM_CONVENTIONAL:
                    tableDesc += "Swarm ";
                    break;
                case HIT_ABOVE:
                    tableDesc += "Above ";
                    break;
                case HIT_BELOW:
                    tableDesc += "Below ";
                    break;
                case HIT_PARTIAL_COVER:
                    tableDesc += "Partial cover " +
                          "(" + LosEffects.getCoverName(cover, true) + ") ";
                    break;
            }
            return " (using " + tableDesc + "table)";
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
     * Determines whether the Margin of success or failure for a given roll. MoS returns a positive while MoF returns a
     * negative
     *
     * @return <code>int</code>
     */
    public int getMoS() {
        return marginOfSuccess;
    }

    public void setMoS(int moS) {
        marginOfSuccess = moS;
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

    public IBuilding getCoverBuildingPrimary() {
        return coverBuildingPrimary;
    }

    public void setCoverBuildingPrimary(IBuilding coverBuilding) {
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

    public IBuilding getCoverBuildingSecondary() {
        return coverBuildingSecondary;
    }

    public void setCoverBuildingSecondary(IBuilding coverBuilding) {
        coverBuildingSecondary = coverBuilding;
    }

    public Coords getCoverLocSecondary() {
        return coverLocSecondary;
    }

    public void setCoverLocSecondary(Coords coverLoc) {
        coverLocSecondary = coverLoc;
    }

    public IBuilding getThruBldg() {
        return thruBldg;
    }

    public void setThruBldg(IBuilding b) {
        thruBldg = b;
    }

    /**
     * Adds to the accumulated Enhanced Imaging (EI) terrain reduction. Call this instead of directly adding an EI
     * modifier. Use {@link #finalizeEiModifier()} to add the combined EI modifier after all terrain calculations are
     * complete.
     *
     * @param amount the EI reduction amount to add (should be positive)
     */
    public void addEiReduction(int amount) {
        eiReduction += amount;
    }

    /**
     * Returns the accumulated EI terrain reduction.
     *
     * @return the total EI reduction accumulated
     */
    public int getEiReduction() {
        return eiReduction;
    }

    /**
     * Adds the accumulated EI terrain reduction as a single modifier, if any reduction has been accumulated. Call this
     * after all terrain modifiers have been added. This method clears the accumulated reduction after adding the
     * modifier.
     */
    public void finalizeEiModifier() {
        if (eiReduction > 0) {
            addModifier(-eiReduction, "EI");
            eiReduction = 0;
        }
    }

    /**
     * Appends another TargetRoll's modifiers to this one. If the other is a ToHitData, also combines the EI
     * reductions.
     *
     * @param other               the TargetRoll to append (may be null)
     * @param appendNonCumulative True to append all modifiers, false to append only cumulative
     */
    @Override
    public void append(TargetRoll other, boolean appendNonCumulative) {
        super.append(other, appendNonCumulative);
        if (other instanceof ToHitData toHitOther) {
            this.eiReduction += toHitOther.eiReduction;
        }
    }

    /**
     * Remove extraneous mods from a ToHitData instance in preparation for recalculating mods.
     */
    public void adjustSwarmToHit() {
        removeModifiers(
              List.of(
                    // Remove first movement modifier (this does not need localization yet; see Compute.getTargetMovementModifier())
                    "target (did not |)move(d \\S* hex)?",
                    // Remove jumped/airborne
                    "target jumped",
                    "target was airborne",
                    // Remove assault dropped
                    "target is assault dropping",
                    // Remove skidded
                    "target skidded",
                    // Remove possible Aerospace Side Mod (these all need to be localized)
                    megamek.client.ui.Messages.getString("WeaponAttackAction.AeroNoseAttack")
                          + "|"
                          + megamek.client.ui.Messages.getString("WeaponAttackAction.AeroSideAttack"),
                    // Remove possible called shot mods
                    megamek.client.ui.Messages.getString("WeaponAttackAction.CalledHigh"),
                    megamek.client.ui.Messages.getString("WeaponAttackAction.CalledLow"),
                    megamek.client.ui.Messages.getString("WeaponAttackAction.CalledLeft"),
                    megamek.client.ui.Messages.getString("WeaponAttackAction.CalledRight"),
                    // Remove target prone mods:
                    megamek.client.ui.Messages.getString("WeaponAttackAction.ProneAdj"),
                    megamek.client.ui.Messages.getString("WeaponAttackAction.ProneRange")
              )
        );
    }

}
