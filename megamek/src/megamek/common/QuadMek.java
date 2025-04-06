/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org), Cord Awtry (kipsta@bs-interactive.com)
 * Copyright (c) 2024, 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import java.io.PrintWriter;
import java.io.Serial;
import java.util.List;
import java.util.stream.IntStream;

import megamek.common.enums.AimingMode;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

public class QuadMek extends Mek {
    @Serial
    private static final long serialVersionUID = 7183093787457804717L;
    private static final MMLogger logger = MMLogger.create(QuadMek.class);

    private static final String[] LOCATION_NAMES = { "Head", "Center Torso", "Right Torso", "Left Torso",
                                                     "Front Right Leg", "Front Left Leg", "Rear Right Leg",
                                                     "Rear Left Leg" };

    private static final String[] LOCATION_ABBRS = { "HD", "CT", "RT", "LT", "FRL", "FLL", "RRL", "RLL" };

    private static final int[] NUM_OF_SLOTS = { 6, 12, 12, 12, 6, 6, 6, 6 };

    public QuadMek(String inGyroType, String inCockpitType) {
        this(Mek.getGyroTypeForString(inGyroType), Mek.getCockpitTypeForString(inCockpitType));
    }

    public QuadMek() {
        this(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD);
    }

    public QuadMek(int inGyroType, int inCockpitType) {
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
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();

        int legsDestroyed = 0;
        int hipHits = 0;
        int actuatorHits = 0;

        // A Mek using tracks has its movement reduced by 25% per leg or track
        // destroyed.
        if (movementMode == EntityMovementMode.TRACKED) {
            for (Mounted<?> m : getMisc()) {
                if (m.getType().hasFlag(MiscType.F_TRACKS)) {
                    if (m.isHit() || isLocationBad(m.getLocation())) {
                        legsDestroyed++;
                    }
                }
            }
            mp = (mp * (4 - legsDestroyed)) / 4;
        } else {
            for (int i = 0; i < locations(); i++) {
                if (locationIsLeg(i)) {
                    if (!isLocationBad(i)) {
                        if (legHasHipCrit(i)) {
                            hipHits++;
                            if ((game == null) ||
                                      !game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
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
                    mp--;
                } else if (legsDestroyed == 2) {
                    mp = 1;
                } else {
                    mp = 0;
                }
            }
            if (mp > 0) {
                if (hipHits > 0) {
                    if ((game != null) &&
                              game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
                        mp = mp - (2 * hipHits);
                    } else {
                        for (int i = 0; i < hipHits; i++) {
                            mp = (int) Math.ceil(mp / 2.0);
                        }
                    }
                }
                mp -= actuatorHits;
            }
        }

        if (!mpCalculationSetting.ignoreModularArmor && hasModularArmor()) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreChainDrape && hasChainDrape()) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreHeat) {
            // factor in heat
            if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT)) {
                if (heat < 30) {
                    mp -= (heat / 5);
                } else if (heat >= 49) {
                    mp -= 9;
                } else if (heat >= 43) {
                    mp -= 8;
                } else if (heat >= 37) {
                    mp -= 7;
                } else if (heat >= 31) {
                    mp -= 6;
                } else {
                    mp -= 5;
                }
            } else {
                mp -= (heat / 5);
            }
        }

        // TSM negates some heat, but provides no benefit when using tracks.
        if (((heat >= 9) || mpCalculationSetting.forceTSM) &&
                  hasTSM(false) &&
                  (legsDestroyed < 2) &&
                  !movementMode.isTracked() &&
                  !movementMode.isWheeled()) {
            if (mpCalculationSetting.forceTSM && mpCalculationSetting.ignoreHeat) {
                // When forcing TSM but ignoring heat we must assume heat to be 9 to activate
                // TSM, this adds -1 MP!
                mp += 1;
            } else {
                mp += 2;
            }
        }

        if (!mpCalculationSetting.ignoreWeather && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);

            if (getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND) &&
                      conditions.getWeather().isClear() &&
                      conditions.getWind().isTornadoF1ToF3()) {
                mp += 1;
            }
        }

        if (!mpCalculationSetting.ignoreGravity) {
            mp = applyGravityEffectsOnMP(mp);
        }

        return Math.max(0, mp);
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        if (countBadLegs() <= 1 ||
                  (this instanceof QuadVee && getConversionMode() == QuadVee.CONV_MODE_VEHICLE && !convertingNow)) {
            return super.getRunMP(mpCalculationSetting);
        } else {
            return getWalkMP(mpCalculationSetting);
        }
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return hasQuirk(OptionsConstants.QUIRK_POS_EXT_TWIST) && !(isProne() || getAlreadyTwisted());
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

    @Override
    public boolean locationIsLeg(int loc) {
        return ((loc == Mek.LOC_RLEG) || (loc == Mek.LOC_LLEG) || (loc == Mek.LOC_RARM) || (loc == Mek.LOC_LARM));
    }

    @Override
    public int getWeaponArc(int wn) {
        final Mounted<?> mounted = getEquipment(wn);

        // B-Pods need to be special-cased, the have 360 firing arc
        if ((mounted.getType() instanceof WeaponType) && mounted.getType().hasFlag(WeaponType.F_B_POD)) {
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
        return switch (mounted.getLocation()) {
            case LOC_HEAD, LOC_CT, LOC_RT, LOC_LT, LOC_RLEG, LOC_LLEG, LOC_LARM, LOC_RARM -> Compute.ARC_FORWARD;
            default -> Compute.ARC_360;
        };
    }

    @Override
    public boolean isQuadMek() {
        return true;
    }

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

    @Override
    public boolean needsRollToStand() {
        return countBadLegs() != 0;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        int[] locsToCheck;
        int destroyedLegs;

        locsToCheck = new int[4];
        locsToCheck[0] = Mek.LOC_RLEG;
        locsToCheck[1] = Mek.LOC_LLEG;
        locsToCheck[2] = Mek.LOC_RARM;
        locsToCheck[3] = Mek.LOC_LARM;

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
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) {
                    roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                    if (!game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
                        continue;
                    }
                }
                // upper leg actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Upper Leg Actuator destroyed");
                }
                // lower leg actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Lower Leg Actuator destroyed");
                }
                // foot actuators?
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_FOOT, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Foot Actuator destroyed");
                }
            }
        }

        return super.addEntityBonuses(roll);
    }

    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
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
        // If we need to abbreviate something that occupies all leg locations, simply
        // return "Legs"
        if ((locations.size() > limit) && (locations.size() == 4) && locations.stream().allMatch(this::locationIsLeg)) {
            return "Legs";
        } else {
            return super.joinLocationAbbr(locations, limit);
        }
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        int roll;

        if ((aimedLocation != LOC_NONE) && !aimingMode.isNone()) {
            roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MEK_HIT_LOCATIONS)) {
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
                    logger.error("", t);
                }

                if (side == ToHitData.SIDE_FRONT) {
                    // normal front hits
                    switch (roll) {
                        case 2:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_TAC) &&
                                      !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side, Mek.LOC_CT, cover, false));
                                return result;
                            }
                            return tac(table, side, Mek.LOC_CT, cover, false);
                        case 3:
                            return new HitData(Mek.LOC_LLEG);
                        case 4:
                        case 5:
                            return new HitData(Mek.LOC_LARM);
                        case 6:
                            return new HitData(Mek.LOC_LT);
                        case 7:
                            return new HitData(Mek.LOC_CT);
                        case 8:
                            return new HitData(Mek.LOC_RT);
                        case 9:
                        case 10:
                            return new HitData(Mek.LOC_RARM);
                        case 11:
                            return new HitData(Mek.LOC_RLEG);
                        case 12:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(Mek.LOC_HEAD));
                                return result;
                            }
                            return new HitData(Mek.LOC_HEAD);
                    }
                } else if (side == ToHitData.SIDE_REAR) {
                    switch (roll) {
                        case 2:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_TAC) &&
                                      !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side, Mek.LOC_CT, cover, true));
                                return result;
                            }
                            return tac(table, side, Mek.LOC_CT, cover, true);
                        case 3:
                            return new HitData(Mek.LOC_LARM, true);
                        case 4:
                        case 5:
                            return new HitData(Mek.LOC_LLEG, true);
                        case 6:
                            return new HitData(Mek.LOC_LT, true);
                        case 7:
                            return new HitData(Mek.LOC_CT, true);
                        case 8:
                            return new HitData(Mek.LOC_RT, true);
                        case 9:
                        case 10:
                            return new HitData(Mek.LOC_RLEG, true);
                        case 11:
                            return new HitData(Mek.LOC_RARM, true);
                        case 12:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(Mek.LOC_HEAD, true));
                                return result;
                            }
                            return new HitData(Mek.LOC_HEAD, true);
                    }
                } else if (side == ToHitData.SIDE_LEFT) {
                    switch (roll) {
                        case 2:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_TAC) &&
                                      !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side, Mek.LOC_LT, cover, false));
                                return result;
                            }
                            return tac(table, side, Mek.LOC_LT, cover, false);
                        case 3:
                            return new HitData(Mek.LOC_RARM);
                        case 4:
                        case 5:
                            return new HitData(Mek.LOC_LARM);
                        case 6:
                            return new HitData(Mek.LOC_RT);
                        case 7:
                            return new HitData(Mek.LOC_LT);
                        case 8:
                            return new HitData(Mek.LOC_CT);
                        case 9:
                        case 10:
                            return new HitData(Mek.LOC_LLEG);
                        case 11:
                            return new HitData(Mek.LOC_RLEG);
                        case 12:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(Mek.LOC_HEAD));
                                return result;
                            }
                            return new HitData(Mek.LOC_HEAD);
                    }
                } else if (side == ToHitData.SIDE_RIGHT) {
                    switch (roll) {
                        case 2:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_TAC) &&
                                      !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side, Mek.LOC_RT, cover, false));
                                return result;
                            }
                            return tac(table, side, Mek.LOC_RT, cover, false);
                        case 3:
                            return new HitData(Mek.LOC_LARM);
                        case 4:
                        case 5:
                            return new HitData(Mek.LOC_RARM);
                        case 6:
                            return new HitData(Mek.LOC_CT);
                        case 7:
                            return new HitData(Mek.LOC_RT);
                        case 8:
                            return new HitData(Mek.LOC_LT);
                        case 9:
                        case 10:
                            return new HitData(Mek.LOC_RLEG);
                        case 11:
                            return new HitData(Mek.LOC_LLEG);
                        case 12:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(Mek.LOC_HEAD));
                                return result;
                            }
                            return new HitData(Mek.LOC_HEAD);
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
                logger.error("", t);
            }

            if (side == ToHitData.SIDE_FRONT) {
                switch (roll) {
                    case 1:
                        return new HitData(Mek.LOC_LARM);
                    case 2:
                        return new HitData(Mek.LOC_LT);
                    case 3:
                        return new HitData(Mek.LOC_CT);
                    case 4:
                        return new HitData(Mek.LOC_RT);
                    case 5:
                        return new HitData(Mek.LOC_RARM);
                    case 6:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mek.LOC_HEAD, true));
                            return result;
                        }
                        return new HitData(Mek.LOC_HEAD, true);
                }
            } else if (side == ToHitData.SIDE_REAR) {
                switch (roll) {
                    case 1:
                        return new HitData(Mek.LOC_LLEG, true);
                    case 2:
                        return new HitData(Mek.LOC_LT, true);
                    case 3:
                        return new HitData(Mek.LOC_CT, true);
                    case 4:
                        return new HitData(Mek.LOC_RT, true);
                    case 5:
                        return new HitData(Mek.LOC_RLEG, true);
                    case 6:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mek.LOC_HEAD, true));
                            return result;
                        }
                        return new HitData(Mek.LOC_HEAD, true);
                }
            } else if (side == ToHitData.SIDE_LEFT) {
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_LT);
                    case 3:
                        return new HitData(Mek.LOC_CT);
                    case 4:
                        return new HitData(Mek.LOC_LARM);
                    case 5:
                        return new HitData(Mek.LOC_LLEG);
                    case 6:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mek.LOC_HEAD, true));
                            return result;
                        }
                        return new HitData(Mek.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_RIGHT) {
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_RT);
                    case 3:
                        return new HitData(Mek.LOC_CT);
                    case 4:
                        return new HitData(Mek.LOC_RARM);
                    case 5:
                        return new HitData(Mek.LOC_RLEG);
                    case 6:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mek.LOC_HEAD, true));
                            return result;
                        }
                        return new HitData(Mek.LOC_HEAD);
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
                logger.error("", t);
            }

            boolean left = (roll <= 3);
            if (side == ToHitData.SIDE_FRONT) {
                if (left) {
                    return new HitData(Mek.LOC_LARM);
                }
                return new HitData(Mek.LOC_RARM);
            } else if (side == ToHitData.SIDE_REAR) {
                if (left) {
                    return new HitData(Mek.LOC_LLEG);
                }
                return new HitData(Mek.LOC_RLEG);
            } else if (side == ToHitData.SIDE_LEFT) {
                if (left) {
                    return new HitData(Mek.LOC_LLEG);
                }
                return new HitData(Mek.LOC_LARM);
            } else if (side == ToHitData.SIDE_RIGHT) {
                if (left) {
                    return new HitData(Mek.LOC_RARM);
                }
                return new HitData(Mek.LOC_RLEG);
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
                logger.error("", t);
            }
            // Swarm attack locations.
            switch (roll) {
                case 2:
                    if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                        getCrew().decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                        result.setUndoneLocation(new HitData(Mek.LOC_HEAD, false, effects));
                        return result;
                    }
                    return new HitData(Mek.LOC_HEAD, false, effects);
                case 3:
                    return new HitData(Mek.LOC_RT, false, effects);
                case 4:
                    return new HitData(Mek.LOC_CT, true, effects);
                case 5:
                    return new HitData(Mek.LOC_RT, true, effects);
                case 6:
                    return new HitData(Mek.LOC_RT, false, effects);
                case 7:
                    return new HitData(Mek.LOC_CT, false, effects);
                case 8:
                    return new HitData(Mek.LOC_LT, false, effects);
                case 9:
                    return new HitData(Mek.LOC_LT, true, effects);
                case 10:
                    return new HitData(Mek.LOC_CT, true, effects);
                case 11:
                    return new HitData(Mek.LOC_LT, false, effects);
                case 12:
                    if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                        getCrew().decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                        result.setUndoneLocation(new HitData(Mek.LOC_HEAD, false, effects));
                        return result;
                    }
                    return new HitData(Mek.LOC_HEAD, false, effects);
            }
        }
        return super.rollHitLocation(table, side, aimedLocation, aimingMode, cover);
    }

    @Override
    public boolean removePartialCoverHits(int location, int cover, int side) {
        // treat front legs like legs not arms.

        // Handle upper cover specially, as treating it as a bitmask will lead
        // to every location being covered
        if (cover == LosEffects.COVER_UPPER) {
            return (location != LOC_LLEG) && (location != LOC_RLEG) && (location != LOC_LARM) && (location != LOC_RARM);
        }

        // left and right cover are from attacker's POV.
        // if hitting front arc, need to swap them
        if (side == ToHitData.SIDE_FRONT) {
            if (((cover & LosEffects.COVER_LOWRIGHT) != 0) &&
                      ((location == Mek.LOC_LARM) || (location == Mek.LOC_LLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOWLEFT) != 0) &&
                      ((location == Mek.LOC_RARM) || (location == Mek.LOC_RLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_RIGHT) != 0) &&
                      ((location == Mek.LOC_LARM) || (location == Mek.LOC_LT) || (location == Mek.LOC_LLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LEFT) != 0) &&
                      ((location == Mek.LOC_RARM) || (location == Mek.LOC_RT) || (location == Mek.LOC_RLEG))) {
                return true;
            }
        } else {
            if (((cover & LosEffects.COVER_LOWLEFT) != 0) &&
                      ((location == Mek.LOC_LARM) || (location == Mek.LOC_LLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LOWRIGHT) != 0) &&
                      ((location == Mek.LOC_RARM) || (location == Mek.LOC_RLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_LEFT) != 0) &&
                      ((location == Mek.LOC_LARM) || (location == Mek.LOC_LT) || (location == Mek.LOC_LLEG))) {
                return true;
            }
            if (((cover & LosEffects.COVER_RIGHT) != 0) &&
                      ((location == Mek.LOC_RARM) || (location == Mek.LOC_RT) || (location == Mek.LOC_RLEG))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canGoHullDown() {
        // check the option
        boolean retVal = game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN);
        if (!retVal) {
            return false;
        }
        // check the locations
        int[] locations = { Mek.LOC_RARM, Mek.LOC_LARM, Mek.LOC_LLEG, Mek.LOC_RLEG };
        int badLocs = 0;
        for (int loc = locations.length - 1; loc >= 0; loc--) {
            if (isLocationBad(locations[loc]) || isLocationDoomed(locations[loc])) {
                badLocs++;
            }
        }

        return (badLocs < 2) && !isGyroDestroyed();
    }

    @Override
    public boolean isArm(int loc) {
        return false;
    }

    @Override
    public boolean hasMPReducingHardenedArmor() {
        return IntStream.of(LOC_LLEG, LOC_RLEG, LOC_LARM, LOC_RARM)
                     .anyMatch(i -> (armorType[i] == EquipmentType.T_ARMOR_HARDENED));
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_MEK | Entity.ETYPE_QUAD_MEK;
    }

    @Override
    public boolean hasClaw(int location) {
        return false;
    }

    @Override
    protected int legCount() {
        return 4;
    }

    @Override
    public void clearInitiative(boolean bUseInitComp) {

    }
}
