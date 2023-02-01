/*
 * MegaMek -
 * Copyright (C) 2000-2002
 *  Ben Mazur (bmazur@sev.org)
 *  Cord Awtry (kipsta@bs-interactive.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.common.enums.AimingMode;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import org.apache.logging.log4j.LogManager;

import java.io.PrintWriter;
import java.util.List;
import java.util.stream.IntStream;

public class QuadMech extends Mech {
    private static final long serialVersionUID = 7183093787457804717L;

    private static final String[] LOCATION_NAMES = { "Head", "Center Torso", "Right Torso", "Left Torso", "Front Right Leg", "Front Left Leg", "Rear Right Leg", "Rear Left Leg" };

    private static final String[] LOCATION_ABBRS = { "HD", "CT", "RT", "LT", "FRL", "FLL", "RRL", "RLL" };

    private static final int[] NUM_OF_SLOTS = { 6, 12, 12, 12, 6, 6, 6, 6 };

    public QuadMech(String inGyroType, String inCockpitType) {
        this(Mech.getGyroTypeForString(inGyroType), Mech.getCockpitTypeForString(inCockpitType));
    }

    public QuadMech() {
        this(Mech.GYRO_STANDARD, Mech.COCKPIT_STANDARD);
    }

    public QuadMech(int inGyroType, int inCockpitType) {
        super(inGyroType, inCockpitType);

        movementMode = EntityMovementMode.QUAD;
        originalMovementMode = EntityMovementMode.QUAD;

        setCritical(LOC_RARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_RARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_RARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_RARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));

        setCritical(LOC_LARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_LARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_LARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_LARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));
    }

    /**
     * Returns true if the Mech cannot stand up any longer.
     */
    @Override
    public boolean cannotStandUpFromHullDown() {
        int i = 0;
        if (isLocationBad(LOC_LARM)) {
            i++;
        }
        if (isLocationBad(LOC_RARM)) {
            i++;
        }
        if (isLocationBad(LOC_LLEG)) {
            i++;
        }
        if (isLocationBad(LOC_RLEG)) {
            i++;
        }
        return i >= 3;
    }

    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int wmp = getOriginalWalkMP();

        int legsDestroyed = 0;
        int hipHits = 0;
        int actuatorHits = 0;

        // A Mech using tracks has its movement reduced by 25% per leg or track destroyed.
        if (movementMode == EntityMovementMode.TRACKED) {
            for (Mounted m : getMisc()) {
                if (m.getType().hasFlag(MiscType.F_TRACKS)) {
                    if (m.isHit() || isLocationBad(m.getLocation())) {
                        legsDestroyed++;
                    }
                }
            }
            wmp = (wmp * (4 - legsDestroyed)) / 4;
        } else {
            for (int i = 0; i < locations(); i++) {
                if (locationIsLeg(i)) {
                    if (!isLocationBad(i)) {
                        if (legHasHipCrit(i)) {
                            hipHits++;
                            if ((game == null) || !game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
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
                if (legsDestroyed == 1) {
                    wmp--;
                } else if (legsDestroyed == 2) {
                    wmp = 1;
                } else {
                    wmp = 0;
                }
            }        
            if (wmp > 0) {
                if (hipHits > 0) {
                    if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
                        wmp = wmp - (2 * hipHits);
                    } else {
                        for (int i = 0; i < hipHits; i++) {
                            wmp = (int) Math.ceil(wmp / 2.0);
                        }
                    }
                }
                wmp -= actuatorHits;
            }
        }

        if (!ignoremodulararmor && hasModularArmor()) {
            wmp--;
        }

        if (!ignoreheat) {
            // factor in heat
            if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT)) {
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
            // TSM negates some heat but has no benefit for 'Mechs using tracks or QuadVees in vehicle mode.
            if ((heat >= 9) && hasTSM(false) && legsDestroyed < 2
                    && movementMode != EntityMovementMode.TRACKED
                    && movementMode != EntityMovementMode.WHEELED) {
                wmp += 2;
            }
        }
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if (weatherMod != 0) {
                wmp = Math.max(wmp + weatherMod, 0);
            }

            if(getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND)
                    && (game.getPlanetaryConditions().getWeather() == PlanetaryConditions.WI_TORNADO_F13)) {
                wmp += 1;
            }
        }
        // gravity
        if (gravity) {
            wmp = applyGravityEffectsOnMP(wmp);
        }
        // For sanity sake...
        wmp = Math.max(0, wmp);
        return wmp;
    }

    /**
     * @return this mek's running/flank mp modified for leg loss and stuff.
     */
    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if (countBadLegs() <= 1
                || (this instanceof QuadVee && getConversionMode() == QuadVee.CONV_MODE_VEHICLE
                && !convertingNow)) {
            return super.getRunMP(gravity, ignoreheat, ignoremodulararmor);
        }
        return getWalkMP(gravity, ignoreheat, ignoremodulararmor);
    }

    /**
     * Returns run MP without considering MASC modified for leg loss and stuff.
     */
    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if (countBadLegs() <= 1
                || (this instanceof QuadVee && getConversionMode() == QuadVee.CONV_MODE_VEHICLE
                && !convertingNow)) {
            return super.getRunMPwithoutMASC(gravity, ignoreheat, ignoremodulararmor);
        }
        return getWalkMP(gravity, ignoreheat);
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return hasQuirk(OptionsConstants.QUIRK_POS_EXT_TWIST) && !isProne();
    }
    
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            return (rotate <= 1) || (rotate == 5);
        } else {
            return rotate == 0;
        }
    }

    /**
     * Returns true is the location is a leg
     */
    @Override
    public boolean locationIsLeg(int loc) {
        return ((loc == Mech.LOC_RLEG) || (loc == Mech.LOC_LLEG) || (loc == Mech.LOC_RARM) || (loc == Mech.LOC_LARM));
    }

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);

        // B-Pods need to be special-cased, the have 360 firing arc
        if ((mounted.getType() instanceof WeaponType) &&
                mounted.getType().hasFlag(WeaponType.F_B_POD)) {
            return Compute.ARC_360;
        }
        // VGLs base arc on their facing
        if (mounted.getType().hasFlag(WeaponType.F_VGL)) {
            return Compute.firingArcFromVGLFacing(mounted.getFacing());
        }
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
    @Override
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
    @Override
    public boolean needsRollToStand() {
        return countBadLegs() != 0;
    }

    /**
     * Add in any piloting skill mods
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        int[] locsToCheck = new int[4];
        int destroyedLegs = 0;

        locsToCheck = new int[4];
        locsToCheck[0] = Mech.LOC_RLEG;
        locsToCheck[1] = Mech.LOC_LLEG;
        locsToCheck[2] = Mech.LOC_RARM;
        locsToCheck[3] = Mech.LOC_LARM;

        destroyedLegs = countBadLegs();

        // QuadVees lose the bonus when converting.
        if (destroyedLegs == 0 && !convertingNow) {
            roll.addModifier(-2, "Quad bonus");
        }

        if (hasAbility(OptionsConstants.PILOT_ANIMAL_MIMIC)) {
            roll.addModifier(-1, "Animal Mimicry");
        }

        if (hasFunctionalLegAES()) {
            roll.addModifier(-2, "AES bonus");
        }

        boolean destroyedLegCounted = false;
        for (int loc : locsToCheck) {
            if (isLocationBad(loc)) {
                // a quad with 2 destroyed legs acts like a biped with one leg
                // destroyed, so add the +5 only once
                // 3 or more destroyed legs are being taken care in
                // getBasePiloting
                if ((destroyedLegs == 2) && !destroyedLegCounted) {
                    roll.addModifier(5, "2 legs destroyed");
                    destroyedLegCounted = true;
                }
            } else {
                // check for damaged hip actuators
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) > 0) {
                    roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                    if (!game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
                        continue;
                    }
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
    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Returns a vector of names for all locations
     */
    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Returns a vector of abbreviations for all locations
     */
    @Override
    public String[] getLocationAbbrs() {
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

    @Override
    protected double getArmActuatorCost() {
        return 0;
    }

    @Override
    protected double getLegActuatorCost() {
        return (weight * 150 * 4) + (weight * 80 * 4) + (weight * 120 * 4);
    }

    @Override
    public String joinLocationAbbr(List<Integer> locations, int limit) {
        // If we need to abbreviate something that occupies all leg locations, simply return "Legs"
        if ((locations.size() > limit) && (locations.size() == 4) && locations.stream().allMatch(this::locationIsLeg)) {
            return "Legs";
        } else {
            return super.joinLocationAbbr(locations, limit);
        }
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
                                   int cover) {
        int roll;

        if ((aimedLocation != LOC_NONE) && !aimingMode.isNone()) {
            roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MECH_HIT_LOCATIONS)) {
            if ((table == ToHitData.HIT_NORMAL) || (table == ToHitData.HIT_PARTIAL_COVER)) {
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
                } catch (Throwable t) {
                    LogManager.getLogger().error("", t);
                }

                if (side == ToHitData.SIDE_FRONT) {
                    // normal front hits
                    switch (roll) {
                        case 2:
                            if ((getCrew().hasEdgeRemaining()
                                    && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_TAC))
                                    && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side, Mech.LOC_CT, cover, false));
                                return result;
                            }
                            return tac(table, side, Mech.LOC_CT, cover, false);
                        case 3:
                            return new HitData(Mech.LOC_LLEG);
                        case 4:
                        case 5:
                            return new HitData(Mech.LOC_LARM);
                        case 6:
                            return new HitData(Mech.LOC_LT);
                        case 7:
                            return new HitData(Mech.LOC_CT);
                        case 8:
                            return new HitData(Mech.LOC_RT);
                        case 9:
                        case 10:
                            return new HitData(Mech.LOC_RARM);
                        case 11:
                            return new HitData(Mech.LOC_RLEG);
                        case 12:
                            if ((getCrew().hasEdgeRemaining()
                                    && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_HEADHIT))) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                                return result;
                            }
                            return new HitData(Mech.LOC_HEAD);
                    }
                } else if (side == ToHitData.SIDE_REAR) {
                    switch (roll) {
                        case 2:
                            if ((getCrew().hasEdgeRemaining()
                                    && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_TAC))
                                    && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side, Mech.LOC_CT, cover, true));
                                return result;
                            }
                            return tac(table, side, Mech.LOC_CT, cover, true);
                        case 3:
                            return new HitData(Mech.LOC_LARM, true);
                        case 4:
                        case 5:
                            return new HitData(Mech.LOC_LLEG, true);
                        case 6:
                            return new HitData(Mech.LOC_LT, true);
                        case 7:
                            return new HitData(Mech.LOC_CT, true);
                        case 8:
                            return new HitData(Mech.LOC_RT, true);
                        case 9:
                        case 10:
                            return new HitData(Mech.LOC_RLEG, true);
                        case 11:
                            return new HitData(Mech.LOC_RARM, true);
                        case 12:
                            if ((getCrew().hasEdgeRemaining()
                                    && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_HEADHIT))) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(Mech.LOC_HEAD, true));
                                return result;
                            }
                            return new HitData(Mech.LOC_HEAD, true);
                    }
                } else if (side == ToHitData.SIDE_LEFT) {
                    switch (roll) {
                        case 2:
                            if ((getCrew().hasEdgeRemaining()
                                    && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_TAC))
                                    && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side, Mech.LOC_LT, cover, false));
                                return result;
                            }
                            return tac(table, side, Mech.LOC_LT, cover, false);
                        case 3:
                            return new HitData(Mech.LOC_RARM);
                        case 4:
                        case 5:
                            return new HitData(Mech.LOC_LARM);
                        case 6:
                            return new HitData(Mech.LOC_RT);
                        case 7:
                            return new HitData(Mech.LOC_LT);
                        case 8:
                            return new HitData(Mech.LOC_CT);
                        case 9:
                        case 10:
                            return new HitData(Mech.LOC_LLEG);
                        case 11:
                            return new HitData(Mech.LOC_RLEG);
                        case 12:
                            if ((getCrew().hasEdgeRemaining()
                                    && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_HEADHIT))) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                                return result;
                            }
                            return new HitData(Mech.LOC_HEAD);
                    }
                } else if (side == ToHitData.SIDE_RIGHT) {
                    switch (roll) {
                        case 2:
                            if ((getCrew().hasEdgeRemaining()
                                    && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_TAC))
                                    && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side, Mech.LOC_RT, cover, false));
                                return result;
                            }
                            return tac(table, side, Mech.LOC_RT, cover, false);
                        case 3:
                            return new HitData(Mech.LOC_LARM);
                        case 4:
                        case 5:
                            return new HitData(Mech.LOC_RARM);
                        case 6:
                            return new HitData(Mech.LOC_CT);
                        case 7:
                            return new HitData(Mech.LOC_RT);
                        case 8:
                            return new HitData(Mech.LOC_LT);
                        case 9:
                        case 10:
                            return new HitData(Mech.LOC_RLEG);
                        case 11:
                            return new HitData(Mech.LOC_LLEG);
                        case 12:
                            if ((getCrew().hasEdgeRemaining()
                                    && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_HEADHIT))) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(Mech.LOC_HEAD));
                                return result;
                            }
                            return new HitData(Mech.LOC_HEAD);
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
            } catch (Throwable t) {
                LogManager.getLogger().error("", t);
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
                        if (getCrew().hasEdgeRemaining()
                                && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD, true));
                            return result;
                        }
                        return new HitData(Mech.LOC_HEAD, true);
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
                        if (getCrew().hasEdgeRemaining()
                                && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD, true));
                            return result;
                        }
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
                        if (getCrew().hasEdgeRemaining()
                                && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD, true));
                            return result;
                        }
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
                        if (getCrew().hasEdgeRemaining()
                                && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mech.LOC_HEAD, true));
                            return result;
                        }
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
            } catch (Throwable t) {
                LogManager.getLogger().error("", t);
            }

            boolean left = (roll <= 3);
            if (side == ToHitData.SIDE_FRONT) {
                if (left) {
                    return new HitData(Mech.LOC_LARM);
                }
                return new HitData(Mech.LOC_RARM);
            } else if (side == ToHitData.SIDE_REAR) {
                if (left) {
                    return new HitData(Mech.LOC_LLEG);
                }
                return new HitData(Mech.LOC_RLEG);
            } else if (side == ToHitData.SIDE_LEFT) {
                if (left) {
                    return new HitData(Mech.LOC_LLEG);
                }
                return new HitData(Mech.LOC_LARM);
            } else if (side == ToHitData.SIDE_RIGHT) {
                if (left) {
                    return new HitData(Mech.LOC_RARM);
                }
                return new HitData(Mech.LOC_RLEG);
            }
        } else if ((table == ToHitData.HIT_SWARM) || (table == ToHitData.HIT_SWARM_CONVENTIONAL)) {
            int effects;
            if (table == ToHitData.HIT_SWARM_CONVENTIONAL) {
                effects = HitData.EFFECT_NONE;
            } else {
                effects = HitData.EFFECT_CRITICAL;
            }
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
            } catch (Throwable t) {
                LogManager.getLogger().error("", t);
            }
            // Swarm attack locations.
            switch (roll) {
                case 2:
                    if (getCrew().hasEdgeRemaining()
                            && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                        getCrew().decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD, false, effects));
                        return result;
                    }
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
                    if (getCrew().hasEdgeRemaining()
                            && getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                        getCrew().decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                        result.setUndoneLocation(new HitData(Mech.LOC_HEAD, false, effects));
                        return result;
                    }
                    return new HitData(Mech.LOC_HEAD, false, effects);
            }
        }
        return super.rollHitLocation(table, side, aimedLocation, aimingMode, cover);
    }

    @Override
    public boolean removePartialCoverHits(int location, int cover, int side) {
        // treat front legs like legs not arms.
        
        // Handle upper cover specially, as treating it as a bitmask will lead
        //  to every location being covered
        if (cover  == LosEffects.COVER_UPPER) {
            if ((location == LOC_LLEG) || (location == LOC_RLEG)
                    || (location == LOC_LARM) || (location == LOC_RARM)) {
                return false;
            } else {
                return true;
            }
        }
        
        // left and right cover are from attacker's POV.
        // if hitting front arc, need to swap them
        if (side == ToHitData.SIDE_FRONT) {
            if (((cover & LosEffects.COVER_LOWRIGHT) != 0) &&
                    ((location == Mech.LOC_LARM) ||
                     (location == Mech.LOC_LLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOWLEFT) != 0) &&
                    ((location == Mech.LOC_RARM) ||
                     (location == Mech.LOC_RLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_RIGHT) != 0) &&
                    ((location == Mech.LOC_LARM) ||
                     (location == Mech.LOC_LT) ||
                     (location == Mech.LOC_LLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LEFT) != 0) &&
                    ((location == Mech.LOC_RARM) ||
                     (location == Mech.LOC_RT) ||
                     (location == Mech.LOC_RLEG))) {
                return true;
            }
        } else {
            if (((cover & LosEffects.COVER_LOWLEFT) != 0) &&
                    ((location == Mech.LOC_LARM) ||
                     (location == Mech.LOC_LLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOWRIGHT) != 0) &&
                    ((location == Mech.LOC_RARM) ||
                     (location == Mech.LOC_RLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LEFT) != 0) &&
                    ((location == Mech.LOC_LARM) ||
                     (location == Mech.LOC_LT) ||
                     (location == Mech.LOC_LLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_RIGHT) != 0) &&
                    ((location == Mech.LOC_RARM) ||
                     (location == Mech.LOC_RT) ||
                     (location == Mech.LOC_RLEG))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for functional AES in all legs
     */
    @Override
    public boolean hasFunctionalLegAES() {
        boolean frontRightLeg = false;
        boolean frontLeftLeg = false;
        boolean rearRightLeg = false;
        boolean rearLeftLeg = false;

        for (Mounted mounted : getMisc()) {
            if (IntStream.of(Mech.LOC_LLEG, Mech.LOC_RLEG, Mech.LOC_LARM, Mech.LOC_RARM)
                    .anyMatch(i -> (mounted.getLocation() == i))) {
                if (mounted.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)
                        && !mounted.isDestroyed() && !mounted.isBreached() && !mounted.isMissing()) {
                    if (mounted.getLocation() == Mech.LOC_LLEG) {
                        rearLeftLeg = true;
                    } else if (mounted.getLocation() == Mech.LOC_RLEG) {
                        rearRightLeg = true;
                    } else if (mounted.getLocation() == Mech.LOC_RARM) {
                        frontRightLeg = true;
                    } else {
                        frontLeftLeg = true;
                    }
                } else if (mounted.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
                    // AES is destroyed, so it cannot be used.
                    return false;
                }
            }
        }

        return frontLeftLeg && frontRightLeg && rearRightLeg && rearLeftLeg;
    }

    @Override
    public boolean canGoHullDown() {
        // check the option
        boolean retVal = game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN);
        if (!retVal) {
            return false;
        }
        // check the locations
        int[] locations = { Mech.LOC_RARM, Mech.LOC_LARM, Mech.LOC_LLEG, Mech.LOC_RLEG };
        int badLocs = 0;
        for ( int loc = locations.length - 1; loc >= 0; loc--) {
            if ( isLocationBad(locations[loc]) || isLocationDoomed(locations[loc])) {
                badLocs++;
            }
        }

        return (badLocs < 2) && !isGyroDestroyed();
    }

    /**
     * Is the passed in location an arm?
     * @param loc
     * @return
     */
    @Override
    public boolean isArm(int loc) {
        // quads don't have arms
        return false;
    }

    @Override
    public boolean hasMPReducingHardenedArmor() {
        return IntStream.of(LOC_LLEG, LOC_RLEG, LOC_LARM, LOC_RARM)
                .anyMatch(i -> (armorType[i] == EquipmentType.T_ARMOR_HARDENED));
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_MECH | Entity.ETYPE_QUAD_MECH;
    }

    /**
     * quad mechs can't have claws
     */
    @Override
    public boolean hasClaw(int location) {
        return false;
    }
}
