/*
 * MegaMek - 
 *  Copyright (C) 2000-2002 
 *    Ben Mazur (bmazur@sev.org)
 *    Cord Awtry (kipsta@bs-interactive.com)
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

import java.io.PrintWriter;

import megamek.common.preference.PreferenceManager;

public class QuadMech extends Mech {
    /**
     * 
     */
    private static final long serialVersionUID = 7183093787457804717L;

    public static final String[] LOCATION_NAMES = { "Head", "Center Torso", "Right Torso", "Left Torso", "Front Right Leg", "Front Left Leg", "Rear Right Leg", "Rear Left Leg" };

    public static final String[] LOCATION_ABBRS = { "HD", "CT", "RT", "LT", "FRL", "FLL", "RRL", "RLL" };

    private static final int[] NUM_OF_SLOTS = { 6, 12, 12, 12, 6, 6, 6, 6 };

    public QuadMech(String inGyroType, String inCockpitType) {
        this(getGyroTypeForString(inGyroType), getCockpitTypeForString(inCockpitType));
    }

    public QuadMech() {
        this(Mech.GYRO_STANDARD, Mech.COCKPIT_STANDARD);
    }

    public QuadMech(int inGyroType, int inCockpitType) {
        super(inGyroType, inCockpitType);

        movementMode = IEntityMovementMode.QUAD;

        setCritical(LOC_RARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_RARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_RARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_RARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));

        setCritical(LOC_LARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_LARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_LARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_LARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));
    }

    /*
     * Returns true if the Mech cannot stand up any longer.
     */
    public boolean cannotStandUp() {
        int i = 0;
        if (isLocationBad(LOC_LARM))
            i++;
        if (isLocationBad(LOC_RARM))
            i++;
        if (isLocationBad(LOC_LLEG))
            i++;
        if (isLocationBad(LOC_RLEG))
            i++;
        return i >= 3;
    }

    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        int wmp = getOriginalWalkMP();
        int legsDestroyed = 0;
        int hipHits = 0;
        int actuatorHits = 0;

        for (int i = 0; i < locations(); i++) {
            if (locationIsLeg(i)) {
                if (!isLocationBad(i)) {
                    if (legHasHipCrit(i)) {
                        hipHits++;
                        if (game == null || !game.getOptions().booleanOption("tacops_leg_damage")) {
                            continue;
                        }
                    }
                    actuatorHits += countLegActuatorCrits(i);
                } else {
                    legsDestroyed++;
                }
            }
        }
        // leg damage effects
        if (legsDestroyed > 0) {
            if (legsDestroyed == 1)
                wmp--;
            else if (legsDestroyed == 2)
                wmp = 1;
            else
                wmp = 0;
        }
        if (wmp > 0) {
            if (hipHits > 0) {
                if (game != null && game.getOptions().booleanOption("tacops_leg_damage")) {
                    wmp = wmp - (2 * hipHits);
                } else {
                    for (int i = 0; i < hipHits; i++) {
                        wmp = (int) Math.ceil(wmp / 2.0);
                    }
                }
            }
            wmp -= actuatorHits;
        }
        
        if ( hasModularArmor() ) {
            wmp--;
        }
        
        if (!ignoreheat) {
            // factor in heat
            if (game != null && game.getOptions().booleanOption("tacops_heat")) {
                if (heat < 30) {
                    wmp -= (heat / 5);
                } else if (heat >= 49) {
                    wmp -= 9;
                } else if (heat >= 43) {
                    wmp -= 8;
                } else if (heat >= 37) {
                    wmp -= 7;
                } else if (heat >= 31) {
                    wmp -= 6;
                } else {
                    wmp -= 5;
                }
            } else {
                wmp -= (heat / 5);
            }
            // TSM negates some heat
            if (heat >= 9 && hasTSM()) {
                wmp += 2;
            }
        }
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if (weatherMod != 0) {
                wmp = Math.max(wmp + weatherMod, 0);
            }
        }
        // gravity
        if (gravity)
            wmp = applyGravityEffectsOnMP(wmp);
        // For sanity sake...
        wmp = Math.max(0, wmp);
        return wmp;
    }

    /**
     * Returns this mech's running/flank mp modified for leg loss & stuff.
     */

    public int getRunMP(boolean gravity, boolean ignoreheat) {
        if (countBadLegs() <= 1) {
            return super.getRunMP(gravity, ignoreheat);
        }
        return getWalkMP(gravity, ignoreheat);
    }

    /**
     * Returns run MP without considering MASC modified for leg loss & stuff.
     */
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat) {
        if (countBadLegs() <= 1) {
            return super.getRunMPwithoutMASC(gravity, ignoreheat);
        }
        return getWalkMP(gravity, ignoreheat);
    }

    public boolean canChangeSecondaryFacing() {
        return false;
    }

    /**
     * Returns true is the location is a leg
     */
    public boolean locationIsLeg(int loc) {
        return ((loc == Mech.LOC_RLEG) || (loc == Mech.LOC_LLEG) || (loc == Mech.LOC_RARM) || (loc == Mech.LOC_LARM));
    }

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);
        // rear mounted?
        if (mounted.isRearMounted()) {
            return Compute.ARC_REAR;
        }
        // front mounted
        switch (mounted.getLocation()) {
        case LOC_HEAD:
        case LOC_CT:
        case LOC_RT:
        case LOC_LT:
        case LOC_RLEG:
        case LOC_LLEG:
        case LOC_LARM:
        case LOC_RARM:
            return Compute.ARC_FORWARD;
        default:
            return Compute.ARC_360;
        }
    }

    /**
     * Sets the internal structure for the mech.
     * 
     * @param head
     *            head
     * @param ct
     *            center torso
     * @param t
     *            right/left torso
     * @param arm
     *            right/left arm
     * @param leg
     *            right/left leg
     */
    public void setInternal(int head, int ct, int t, int arm, int leg) {
        initializeInternal(head, LOC_HEAD);
        initializeInternal(ct, LOC_CT);
        initializeInternal(t, LOC_RT);
        initializeInternal(t, LOC_LT);
        initializeInternal(leg, LOC_RARM);
        initializeInternal(leg, LOC_LARM);
        initializeInternal(leg, LOC_RLEG);
        initializeInternal(leg, LOC_LLEG);
    }

    /**
     * Returns true is the entity needs a roll to stand up
     */
    public boolean needsRollToStand() {
        if (countBadLegs() == 0)
            return false;
        return true;
    }

    /**
     * Add in any piloting skill mods
     */
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        int[] locsToCheck = new int[4];
        int destroyedLegs = 0;

        locsToCheck = new int[4];
        locsToCheck[0] = Mech.LOC_RLEG;
        locsToCheck[1] = Mech.LOC_LLEG;
        locsToCheck[2] = Mech.LOC_RARM;
        locsToCheck[3] = Mech.LOC_LARM;

        destroyedLegs = countBadLegs();

        if (destroyedLegs == 0)
            roll.addModifier(-2, "Quad bonus");

        if (hasFunctionalLegAES()) {
            roll.addModifier(-2, "AES bonus");
        }

        for (int i = 0; i < locsToCheck.length; i++) {
            int loc = locsToCheck[i];

            if (isLocationBad(loc)) {
                if (destroyedLegs > 1)
                    roll.addModifier(5, getLocationName(loc) + " destroyed");
            } else {
                // check for damaged hip actuators
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) > 0) {
                    roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                    if (!game.getOptions().booleanOption("tacops_leg_damage"))
                        continue;
                }
                // upper leg actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Upper Leg Actuator destroyed");
                }
                // lower leg actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Lower Leg Actuator destroyed");
                }
                // foot actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Foot Actuator destroyed");
                }
            }
        }

        return super.addEntityBonuses(roll);
    }

    /**
     * Returns a vector of slot counts for all locations
     */
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Returns a vector of names for all locations
     */
    protected String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Returns a vector of abbreviations for all locations
     */
    protected String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    public static int restrictScore(int location) {
        switch (location) {
        case Mech.LOC_RT:
        case Mech.LOC_LT:
            return 1;
        case Mech.LOC_CT:
            return 2;
        default:
            return 3;
        }
    }

    protected double getArmActuatorCost() {
        return 0;
    }

    protected double getLegActuatorCost() {
        return weight * 150 * 4 + weight * 80 * 4 + weight * 120 * 4;
    }

    public HitData rollHitLocation(int table, int side, int aimedLocation, int aimingMode) {
        int roll = -1;

        if ((aimedLocation != LOC_NONE) && (aimingMode != IAimingModes.AIM_MODE_NONE)) {
            roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
            }
        }

        if (game.getOptions().booleanOption("tacops_advanced_mech_hit_locations")) {
            if (table == ToHitData.HIT_NORMAL || table == ToHitData.HIT_PARTIAL_COVER) {
                roll = Compute.d6(2);
                try {
                    PrintWriter pw = PreferenceManager.getClientPreferences().getMekHitLocLog();
                    if (pw != null) {
                        pw.print(table);
                        pw.print("\t");
                        pw.print(side);
                        pw.print("\t");
                        pw.println(roll);
                    }
                } catch (Throwable thrown) {
                    thrown.printStackTrace();
                }
                if (side == ToHitData.SIDE_FRONT) {
                    // normal front hits
                    switch (roll) {
                    case 2:
                        return tac(table, side, Mech.LOC_CT, false);
                    case 3:
                        return new HitData(Mech.LOC_RARM);
                    case 4:
                        return new HitData(Mech.LOC_RLEG);
                    case 5:
                    case 6:
                        return new HitData(Mech.LOC_RARM);
                    case 7:
                        return new HitData(Mech.LOC_CT);
                    case 8:
                        return new HitData(Mech.LOC_LT);
                    case 9:
                    case 10:
                    case 11:
                        return new HitData(Mech.LOC_LARM);
                    case 12:
                        return new HitData(Mech.LOC_LLEG);
                    }
                } else if (side == ToHitData.SIDE_REAR) {
                    switch (roll) {
                    case 2:
                        return tac(table, side, Mech.LOC_CT, true);
                    case 3:
                    case 4:
                        return new HitData(Mech.LOC_RARM, true);
                    case 5:
                    case 6:
                        return new HitData(Mech.LOC_RLEG, true);
                    case 7:
                        return new HitData(Mech.LOC_CT, true);
                    case 8:
                        return new HitData(Mech.LOC_LT, true);
                    case 9:
                    case 10:
                    case 11:
                        return new HitData(Mech.LOC_LLEG, true);
                    case 12:
                        return new HitData(Mech.LOC_LARM, true);
                    }

                } else if (side == ToHitData.SIDE_LEFT) {
                    switch (roll) {
                    case 2:
                        return tac(table, side, Mech.LOC_LT,false);
                    case 3:
                    case 4:
                        return new HitData(Mech.LOC_LLEG);
                    case 5:
                    case 6:
                        return new HitData(Mech.LOC_LARM);
                    case 7:
                        return new HitData(Mech.LOC_LLEG);
                    case 8:
                        return new HitData(Mech.LOC_CT);
                    case 9:
                        return new HitData(Mech.LOC_RT);
                    case 10:
                    case 11:
                        return new HitData(Mech.LOC_RARM);
                    case 12:
                        return new HitData(Mech.LOC_RLEG);
                    }
                } else if (side == ToHitData.SIDE_RIGHT) {
                    switch (roll) {
                    case 2:
                        return tac(table, side, Mech.LOC_RT,false);
                    case 3:
                    case 4:
                        return new HitData(Mech.LOC_RLEG);
                    case 5:
                    case 6:
                        return new HitData(Mech.LOC_RARM);
                    case 7:
                        return new HitData(Mech.LOC_RLEG);
                    case 8:
                        return new HitData(Mech.LOC_CT);
                    case 9:
                        return new HitData(Mech.LOC_LT);
                    case 10:
                    case 11:
                        return new HitData(Mech.LOC_LARM);
                    case 12:
                        return new HitData(Mech.LOC_LLEG);
                    }
                }
            }
        }

        if (table == ToHitData.HIT_PUNCH) {
            roll = Compute.d6();
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences().getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable thrown) {
                thrown.printStackTrace();
            }
            if (side == ToHitData.SIDE_FRONT) {
                switch (roll) {
                case 1:
                    return new HitData(Mech.LOC_LARM);
                case 2:
                    return new HitData(Mech.LOC_LT);
                case 3:
                    return new HitData(Mech.LOC_CT);
                case 4:
                    return new HitData(Mech.LOC_RT);
                case 5:
                    return new HitData(Mech.LOC_RARM);
                case 6:
                    return new HitData(Mech.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_REAR) {
                switch (roll) {
                case 1:
                    return new HitData(Mech.LOC_LLEG, true);
                case 2:
                    return new HitData(Mech.LOC_LT, true);
                case 3:
                    return new HitData(Mech.LOC_CT, true);
                case 4:
                    return new HitData(Mech.LOC_RT, true);
                case 5:
                    return new HitData(Mech.LOC_RLEG, true);
                case 6:
                    if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD, true));
                        return result;
                    } // if
                    return new HitData(Mech.LOC_HEAD, true);
                }
            } else if (side == ToHitData.SIDE_LEFT) {
                switch (roll) {
                case 1:
                case 2:
                    return new HitData(Mech.LOC_LT);
                case 3:
                    return new HitData(Mech.LOC_CT);
                case 4:
                    return new HitData(Mech.LOC_LARM);
                case 5:
                    return new HitData(Mech.LOC_LLEG);
                case 6:
                    if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD, true));
                        return result;
                    } // if
                    return new HitData(Mech.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_RIGHT) {
                switch (roll) {
                case 1:
                case 2:
                    return new HitData(Mech.LOC_RT);
                case 3:
                    return new HitData(Mech.LOC_CT);
                case 4:
                    return new HitData(Mech.LOC_RARM);
                case 5:
                    return new HitData(Mech.LOC_RLEG);
                case 6:
                    if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                        crew.decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD, true));
                        return result;
                    } // if
                    return new HitData(Mech.LOC_HEAD);
                }
            }
        } else if (table == ToHitData.HIT_KICK) {
            roll = Compute.d6(1);
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences().getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable thrown) {
                thrown.printStackTrace();
            }
            boolean left = (roll <= 3);
            if (side == ToHitData.SIDE_FRONT) {
                if (left)
                    return new HitData(Mech.LOC_LARM);
                return new HitData(Mech.LOC_RARM);
            } else if (side == ToHitData.SIDE_REAR) {
                if (left)
                    return new HitData(Mech.LOC_LLEG);
                return new HitData(Mech.LOC_RLEG);
            } else if (side == ToHitData.SIDE_LEFT) {
                if (left)
                    return new HitData(Mech.LOC_LLEG);
                return new HitData(Mech.LOC_LARM);
            } else if (side == ToHitData.SIDE_RIGHT) {
                if (left)
                    return new HitData(Mech.LOC_RARM);
                return new HitData(Mech.LOC_RLEG);
            }
        } else if (table == ToHitData.HIT_SWARM || table == ToHitData.HIT_SWARM_CONVENTIONAL) {
            int effects;
            if (table == ToHitData.HIT_SWARM_CONVENTIONAL)
                effects = HitData.EFFECT_NONE;
            else
                effects = HitData.EFFECT_CRITICAL;
            roll = Compute.d6(2);
            try {
                PrintWriter pw = PreferenceManager.getClientPreferences().getMekHitLocLog();
                if (pw != null) {
                    pw.print(table);
                    pw.print("\t");
                    pw.print(side);
                    pw.print("\t");
                    pw.println(roll);
                }
            } catch (Throwable thrown) {
                thrown.printStackTrace();
            }
            // Swarm attack locations.
            switch (roll) {
            case 2:
                if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                    crew.decreaseEdge();
                    HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                    result.setUndoneLocation(new HitData(Mech.LOC_HEAD, false, effects));
                    return result;
                } // if
                return new HitData(Mech.LOC_HEAD, false, effects);
            case 3:
                return new HitData(Mech.LOC_RT, false, effects);
            case 4:
                return new HitData(Mech.LOC_CT, true, effects);
            case 5:
                return new HitData(Mech.LOC_RT, true, effects);
            case 6:
                return new HitData(Mech.LOC_RT, false, effects);
            case 7:
                return new HitData(Mech.LOC_CT, false, effects);
            case 8:
                return new HitData(Mech.LOC_LT, false, effects);
            case 9:
                return new HitData(Mech.LOC_LT, true, effects);
            case 10:
                return new HitData(Mech.LOC_CT, true, effects);
            case 11:
                return new HitData(Mech.LOC_LT, false, effects);
            case 12:
                if (crew.hasEdgeRemaining() && crew.getOptions().booleanOption("edge_when_headhit")) {
                    crew.decreaseEdge();
                    HitData result = rollHitLocation(table, side, aimedLocation, aimingMode);
                    result.setUndoneLocation(new HitData(Mech.LOC_HEAD, false, effects));
                    return result;
                } // if
                return new HitData(Mech.LOC_HEAD, false, effects);
            }
        }
        return super.rollHitLocation(table, side, aimedLocation, aimingMode);
    }

    public boolean removePartialCoverHits(int location, int cover, int side) {
        // treat front legs like legs not arms.
        if (((cover & LosEffects.COVER_UPPER) == LosEffects.COVER_UPPER) && (location == Mech.LOC_CT || location == Mech.LOC_HEAD))
            return true;
        // left and right cover are from attacker's POV.
        // if hitting front arc, need to swap them
        if (side == ToHitData.SIDE_FRONT) {
            if ((cover & LosEffects.COVER_LOWRIGHT) != 0 && (location == Mech.LOC_LARM || location == Mech.LOC_LLEG))
                return true;
            if ((cover & LosEffects.COVER_LOWLEFT) != 0 && (location == Mech.LOC_RARM || location == Mech.LOC_RLEG))
                return true;
            if ((cover & LosEffects.COVER_RIGHT) != 0 && location == Mech.LOC_LT)
                return true;
            if ((cover & LosEffects.COVER_LEFT) != 0 && location == Mech.LOC_RT)
                return true;
        } else {
            if ((cover & LosEffects.COVER_LOWLEFT) != 0 && (location == Mech.LOC_LARM || location == Mech.LOC_LLEG))
                return true;
            if ((cover & LosEffects.COVER_LOWRIGHT) != 0 && (location == Mech.LOC_RARM || location == Mech.LOC_RLEG))
                return true;
            if ((cover & LosEffects.COVER_LEFT) != 0 && location == Mech.LOC_LT)
                return true;
            if ((cover & LosEffects.COVER_RIGHT) != 0 && location == Mech.LOC_RT)
                return true;
        }
        return false;
    }

    /**
     * Checks for functional AES in all legs
     */
    public boolean hasFunctionalLegAES() {
        boolean frontRightLeg = false;
        boolean frontLeftLeg = false;
        boolean rearRightLeg = false;
        boolean rearLeftLeg = false;

        for (Mounted mounted : this.getMisc()) {
            if (mounted.getLocation() == Mech.LOC_LLEG || mounted.getLocation() == Mech.LOC_RLEG || mounted.getLocation() == Mech.LOC_LARM || mounted.getLocation() == Mech.LOC_RARM) {
                if (((MiscType) mounted.getType()).hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM) && !mounted.isDestroyed() && !mounted.isBreached() && !mounted.isMissing()) {
                    if (mounted.getLocation() == Mech.LOC_LLEG) {
                        rearLeftLeg = true;
                    } else if (mounted.getLocation() == Mech.LOC_RLEG) {
                        rearRightLeg = true;
                    } else if (mounted.getLocation() == Mech.LOC_RARM) {
                        frontRightLeg = true;
                    } else {
                        frontLeftLeg = true;
                    }

                }// AES is destroyed their for it cannot be used.
                else if (((MiscType) mounted.getType()).hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
                    return false;
                }
            }
        }

        return frontLeftLeg && frontRightLeg && rearRightLeg && rearLeftLeg;
    }

    public boolean canGoHullDown() {
        return game.getOptions().booleanOption("tacops_hull_down") && !isLocationBad(Mech.LOC_LARM) && !isLocationBad(Mech.LOC_RARM) && !isLocationBad(Mech.LOC_LLEG) && !isLocationBad(Mech.LOC_RLEG);
    }
}
