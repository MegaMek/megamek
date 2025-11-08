/*
 * Copyright (C) 2013 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013-2025 - The MegaMek Team. All Rights Reserved.
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

package megamek.common.units;

import java.io.PrintWriter;
import java.io.Serial;
import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.MPCalculationSetting;
import megamek.common.ToHitData;
import megamek.common.compute.Compute;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.common.rolls.PilotingRollData;
import megamek.logging.MMLogger;

public class TripodMek extends MekWithArms {
    @Serial
    private static final long serialVersionUID = 4166375446709772785L;
    private static final MMLogger logger = MMLogger.create(TripodMek.class);

    private static final String[] LOCATION_NAMES = { "Head", "Center Torso", "Right Torso", "Left Torso", "Right Arm",
                                                     "Left Arm", "Right Leg", "Left Leg", "Center Leg" };

    private static final String[] LOCATION_ABBREVIATIONS = { "HD", "CT", "RT", "LT", "RA", "LA", "RL", "LL", "CL" };

    private static final int[] NUM_OF_SLOTS = { 6, 12, 12, 12, 12, 12, 6, 6, 6 };

    public TripodMek(String inGyroType, String inCockpitType) {
        this(getGyroTypeForString(inGyroType), getCockpitTypeForString(inCockpitType));
    }

    public TripodMek() {
        this(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD);
    }

    public TripodMek(int inGyroType, int inCockpitType) {
        super(inGyroType, inCockpitType);

        movementMode = EntityMovementMode.TRIPOD;
        originalMovementMode = EntityMovementMode.TRIPOD;

        setCritical(LOC_RIGHT_ARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_RIGHT_ARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_RIGHT_ARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_RIGHT_ARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));

        setCritical(LOC_LEFT_ARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_LEFT_ARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_LEFT_ARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_LEFT_ARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));

        setCritical(LOC_CENTER_LEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_CENTER_LEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_CENTER_LEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_CENTER_LEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));
    }

    @Override
    public boolean locationIsLeg(int loc) {
        return (loc == LOC_LEFT_LEG) || (loc == LOC_RIGHT_LEG) || (loc == LOC_CENTER_LEG);
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_MEK | Entity.ETYPE_TRIPOD_MEK;
    }

    @Override
    public boolean isTripodMek() {
        return true;
    }

    @Override
    public CrewType defaultCrewType() {
        return isSuperHeavy() ? CrewType.SUPERHEAVY_TRIPOD : CrewType.TRIPOD;
    }

    @Override
    public boolean isImmobile() {
        if (gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
            int legsDestroyed = 0;
            for (int i = 0; i < locations(); i++) {
                if (locationIsLeg(i)) {
                    if (isLocationBad(i)) {
                        legsDestroyed++;
                    }
                }
            }
            if (legsDestroyed == 2) {
                return true;
            }
        }
        return super.isImmobile();
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();
        int legsDestroyed = 0;
        int hipHits = 0;
        int actuatorHits = 0;
        int leftHip = 0;
        int rightHip = 0;
        int centerHip = 0;
        int leftLegActuators = 0;
        int rightLegActuators = 0;
        int centerLegActuators = 0;

        // A Mek using tracks has its movement reduced by 1/3 per leg or track destroyed, based
        // on analogy with biped and quad Meks.
        if (getMovementMode().isTracked()) {
            for (Mounted<?> m : getMisc()) {
                if (m.getType().hasFlag(MiscType.F_TRACKS)) {
                    if (m.isHit() || isLocationBad(m.getLocation())) {
                        legsDestroyed++;
                    }
                }
            }
            mp = (mp * (3 - legsDestroyed)) / 3;
        } else {
            for (int i : List.of(Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_LEG, Mek.LOC_CENTER_LEG)) {
                // PLAYTEST2 leg crits and MP
                if (!(game == null) && gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                    if (!isLocationBad(i)) {
                        if (legHasHipCrit(i)) {
                            if (i == Mek.LOC_LEFT_LEG) {
                                leftHip++;
                            }
                            if (i == Mek.LOC_RIGHT_LEG) {
                                rightHip++;
                            }
                            if (i == Mek.LOC_CENTER_LEG) {
                                centerHip++;
                            }
                            hipHits++;
                            if ((game == null) ||
                                  !gameOptions()
                                        .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE)) {
                                continue;
                            }
                        }
                        if (i == Mek.LOC_LEFT_LEG) {
                            leftLegActuators += countLegActuatorCrits(i);
                        }
                        if (i == Mek.LOC_RIGHT_LEG) {
                            rightLegActuators += countLegActuatorCrits(i);
                        }
                        if (i == Mek.LOC_CENTER_LEG) {
                            centerLegActuators += countLegActuatorCrits(i);
                        }
                        actuatorHits += countLegActuatorCrits(i);
                    } else {
                        legsDestroyed++;
                    }
                } else {
                    if (!isLocationBad(i)) {
                        if (legHasHipCrit(i)) {
                            hipHits++;
                            if ((game == null) ||
                                  !gameOptions()
                                        .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE)) {
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
                if (game != null && gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                    if (legsDestroyed == 2) {mp = 0;}
                } else {
                    mp = (legsDestroyed == 1) ? 1 : 0;
                }
            }

            // PLAYTEST 2 Set leg to half MP, ignore crits to the leg.
            if ((game != null) &&
                  gameOptions().booleanOption(OptionsConstants.PLAYTEST_2) && (mp > 0)) {
                if (hipHits > 0 || legsDestroyed == 1) {
                    int minReduction;
                    int midReduction;
                    int maxReduction;
                    minReduction = (int) Math.ceil(mp / 2.0);
                    midReduction = (int) Math.ceil(minReduction / 2.0);
                    maxReduction = (int) Math.ceil(midReduction / 2.0);

                    if ((hipHits == 1 && legsDestroyed == 0) || (hipHits == 0 && legsDestroyed == 1)) {
                        // Only a single hip or leg
                        mp = mp - minReduction;

                    } else if ((hipHits == 2 && legsDestroyed == 0) || (hipHits == 1 && legsDestroyed == 1)) {
                        // Can only happen if 2 hips are hit and no legs are destroyed
                        mp = mp - minReduction - midReduction;
                    } else if ((hipHits == 3) || (hipHits == 2 && legsDestroyed == 1)) {
                        // Can only happen if all 3 hips are hit and no legs are destroyed
                        // or 2 hips and 1 leg destroyed
                        mp = mp - minReduction - midReduction - maxReduction;
                    }
                    if (leftHip == 0) {
                        mp -= leftLegActuators;
                    }
                    if (rightHip == 0) {
                        mp -= rightLegActuators;
                    }
                    if (centerHip == 0) {
                        mp -= centerLegActuators;
                    }
                } else {
                    mp -= actuatorHits;
                }
            } else {
                if (hipHits > 0 && legsDestroyed == 0) {
                    if ((game != null) &&
                          gameOptions()
                                .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE)) {
                        mp = mp - 2 * hipHits;
                    } else {
                        mp = (hipHits == 1) ? (int) Math.ceil(mp / 2.0) : 0;
                    }
                }
                mp -= actuatorHits;
            }
        }

        if (hasShield()) {
            mp -= getNumberOfShields(MiscType.S_SHIELD_LARGE);
            mp -= getNumberOfShields(MiscType.S_SHIELD_MEDIUM);
        }

        if (!mpCalculationSetting.ignoreModularArmor() && hasModularArmor()) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreChainDrape() && hasChainDrape()) {
            mp--;
        }

        if (!mpCalculationSetting.ignoreHeat()) {
            // factor in heat
            if ((game != null) && gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT)) {
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
            // TSM negates some heat
            if ((heat >= 9) && hasTSM(false) && legsDestroyed == 0 && movementMode != EntityMovementMode.TRACKED) {
                mp += 2;
            }
        }

        if (!mpCalculationSetting.ignoreCargo()) {
            mp = Math.max(mp - getCargoMpReduction(this), 0);
        }

        if (!mpCalculationSetting.ignoreWeather() && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);
            if (getCrew().getOptions()
                  .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                  .equals(Crew.ENVIRONMENT_SPECIALIST_WIND) &&
                  conditions.getWeather().isClear() &&
                  conditions.getWind().isTornadoF1ToF3()) {
                mp += 1;
            }
        }

        if (!mpCalculationSetting.ignoreGravity()) {
            mp = applyGravityEffectsOnMP(mp);
        }

        return Math.max(0, mp);
    }

    @Override
    public void setInternal(int head, int ct, int t, int arm, int leg) {
        initializeInternal(head, LOC_HEAD);
        initializeInternal(ct, LOC_CENTER_TORSO);
        initializeInternal(t, LOC_RIGHT_TORSO);
        initializeInternal(t, LOC_LEFT_TORSO);
        initializeInternal(arm, LOC_RIGHT_ARM);
        initializeInternal(arm, LOC_LEFT_ARM);
        initializeInternal(leg, LOC_RIGHT_LEG);
        initializeInternal(leg, LOC_LEFT_LEG);
        initializeInternal(leg, LOC_CENTER_LEG);
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        if (getCrew().hasDedicatedPilot()) {
            roll.addModifier(-1, "dedicated pilot");
        } else {
            roll.addModifier(2, "pilot incapacitated");
        }

        if (hasFunctionalLegAES()) {
            roll.addModifier(-2, "AES bonus");
        }

        if (countBadLegs() == 0) {
            roll.addModifier(-1, "tripod bonus");
        }

        for (int loc : List.of(LOC_LEFT_LEG, LOC_RIGHT_LEG, LOC_CENTER_LEG)) {
            if (isLocationBad(loc)) {
                roll.addModifier(5, getLocationName(loc) + " destroyed");
            } else {
                if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) {
                    roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                    if (!gameOptions()
                          .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE)) {
                        continue;
                    }
                }
                if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Upper Leg Actuator destroyed");
                }
                if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Lower Leg Actuator destroyed");
                }
                if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_FOOT, loc) > 0) {
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
    public String[] getLocationAbbreviations() {
        return LOCATION_ABBREVIATIONS;
    }

    @Override
    protected double getLegActuatorCost() {
        return weight * 3 * (150 + 80 + 120);
    }

    @Override
    public boolean hasActiveShield(int location, boolean rear) {
        return switch (location) {
            case Mek.LOC_CENTER_TORSO, Mek.LOC_HEAD -> {
                // no rear head location so must be rear CT which is not projected by any shield
                if (rear) {
                    yield false;
                }
                yield hasActiveShield(Mek.LOC_LEFT_ARM) || hasActiveShield(Mek.LOC_RIGHT_ARM);
            }
            // else
            case Mek.LOC_LEFT_ARM, Mek.LOC_LEFT_TORSO, Mek.LOC_LEFT_LEG -> hasActiveShield(Mek.LOC_LEFT_ARM);
            default -> hasActiveShield(Mek.LOC_RIGHT_ARM);
        };
    }

    @Override
    public boolean canGoHullDown() {
        return (game != null) &&
              gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN) &&
              ((!isLocationBad(Mek.LOC_LEFT_LEG) &&
                    !isLocationBad(Mek.LOC_RIGHT_LEG) &&
                    !isLocationBad(LOC_CENTER_LEG) &&
                    !isLocationDoomed(Mek.LOC_LEFT_LEG) &&
                    !isLocationDoomed(Mek.LOC_RIGHT_LEG)) && !isLocationDoomed(LOC_CENTER_LEG)) &&
              !isGyroDestroyed();
    }

    @Override
    public boolean cannotStandUpFromHullDown() {
        return isLocationBad(LOC_LEFT_LEG)
              || isLocationBad(LOC_RIGHT_LEG)
              || isLocationBad(LOC_CENTER_LEG)
              || isGyroDestroyed();
    }

    @Override
    public boolean hasMPReducingHardenedArmor() {
        return (armorType[LOC_LEFT_LEG] == EquipmentType.T_ARMOR_HARDENED) ||
              (armorType[LOC_RIGHT_LEG] == EquipmentType.T_ARMOR_HARDENED) ||
              (armorType[LOC_CENTER_LEG] == EquipmentType.T_ARMOR_HARDENED);
    }

    @Override
    public int locations() {
        return 9;
    }

    @Override
    public String joinLocationAbbr(List<Integer> locations, int limit) {
        // If we need to abbreviate something that occupies all leg locations, simply return "Legs"
        if ((locations.size() > limit) && (locations.size() == 3) && locations.stream().allMatch(this::locationIsLeg)) {
            return "Legs";
        } else {
            return super.joinLocationAbbr(locations, limit);
        }
    }

    @Override
    protected HitData innerRollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        int roll;

        if ((aimedLocation != LOC_NONE) && !aimingMode.isNone()) {

            roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
            }
        }

        boolean playtestLocations = gameOptions().booleanOption(OptionsConstants.PLAYTEST_1);

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

            if (playtestLocations
                  && (side == ToHitData.SIDE_LEFT || side == ToHitData.SIDE_RIGHT)
                  && roll != 2 // clarified on forum, TACs don't go to the CT in this case
            ) {
                return getPlaytestSideLocation(table, side, cover);
            }

            if (side == ToHitData.SIDE_FRONT) {
                // normal front hits
                switch (roll) {
                    case 2:
                        return tac(table, side, Mek.LOC_CENTER_TORSO, cover, false);
                    case 3:
                    case 4:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 5:
                    case 9:
                        int legRoll = Compute.d6();
                        if (legRoll <= 2) {
                            return new HitData(Mek.LOC_RIGHT_LEG);
                        } else if (legRoll <= 4) {
                            return new HitData(Mek.LOC_CENTER_LEG);
                        } else {
                            return new HitData(Mek.LOC_LEFT_LEG);
                        }
                    case 6:
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 7:
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 8:
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 10:
                    case 11:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 12:
                        return new HitData(Mek.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_LEFT) {
                // normal left side hits
                switch (roll) {
                    case 2:
                        return tac(table, side, Mek.LOC_LEFT_TORSO, cover, false);
                    case 3:
                    case 6:
                    case 11:
                        int legRoll = Compute.d6() + 1;
                        if (legRoll <= 2) {
                            return new HitData(Mek.LOC_RIGHT_LEG);
                        } else if (legRoll <= 4) {
                            return new HitData(Mek.LOC_CENTER_LEG);
                        } else {
                            return new HitData(Mek.LOC_LEFT_LEG);
                        }
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 7:
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 8:
                        if (gameOptions()
                              .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_CENTER_TORSO, true);
                        }
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 9:
                        if (gameOptions()
                              .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_RIGHT_TORSO, true);
                        }
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 10:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 12:
                        return new HitData(Mek.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_RIGHT) {
                // normal right side hits
                switch (roll) {
                    case 2:
                        return tac(table, side, Mek.LOC_RIGHT_TORSO, cover, false);
                    case 3:
                    case 6:
                    case 11:
                        int legRoll = Compute.d6() - 1;
                        if (legRoll <= 2) {
                            return new HitData(Mek.LOC_RIGHT_LEG);
                        } else if (legRoll <= 4) {
                            return new HitData(Mek.LOC_CENTER_LEG);
                        } else {
                            return new HitData(Mek.LOC_LEFT_LEG);
                        }
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 7:
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 8:
                        if (gameOptions()
                              .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_CENTER_TORSO, true);
                        }
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 9:
                        if (gameOptions()
                              .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_LEFT_TORSO, true);
                        }
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 10:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 12:
                        return new HitData(Mek.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_REAR) {
                // normal rear hits
                if (gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ADVANCED_MEK_HIT_LOCATIONS)
                      &&
                      isProne()) {
                    switch (roll) {
                        case 2:
                            return tac(table, side, Mek.LOC_CENTER_TORSO, cover, true);
                        case 3:
                            return new HitData(Mek.LOC_RIGHT_ARM, true);
                        case 4:
                        case 5:
                        case 9:
                        case 10:
                            int legRoll = Compute.d6();
                            if (legRoll <= 2) {
                                return new HitData(Mek.LOC_RIGHT_LEG);
                            } else if (legRoll <= 4) {
                                return new HitData(Mek.LOC_CENTER_LEG);
                            } else {
                                return new HitData(Mek.LOC_LEFT_LEG);
                            }
                        case 6:
                            return new HitData(Mek.LOC_RIGHT_TORSO, true);
                        case 7:
                            return new HitData(Mek.LOC_CENTER_TORSO, true);
                        case 8:
                            return new HitData(Mek.LOC_LEFT_TORSO, true);
                        case 11:
                            return new HitData(Mek.LOC_LEFT_ARM, true);
                        case 12:
                            return new HitData(Mek.LOC_HEAD, true);
                    }
                } else {
                    switch (roll) {
                        case 2:
                            return tac(table, side, Mek.LOC_CENTER_TORSO, cover, true);
                        case 3:
                        case 4:
                            return new HitData(Mek.LOC_RIGHT_ARM, true);
                        case 5:
                        case 9:
                            int legRoll = Compute.d6();
                            if (legRoll <= 2) {
                                return new HitData(Mek.LOC_RIGHT_LEG);
                            } else if (legRoll <= 4) {
                                return new HitData(Mek.LOC_CENTER_LEG);
                            } else {
                                return new HitData(Mek.LOC_LEFT_LEG);
                            }
                        case 6:
                            return new HitData(Mek.LOC_RIGHT_TORSO, true);
                        case 7:
                            return new HitData(Mek.LOC_CENTER_TORSO, true);
                        case 8:
                            return new HitData(Mek.LOC_LEFT_TORSO, true);
                        case 10:
                        case 11:
                            return new HitData(Mek.LOC_LEFT_ARM, true);
                        case 12:
                            return new HitData(Mek.LOC_HEAD, true);
                    }
                }
            }
        }
        if (table == ToHitData.HIT_PUNCH) {
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

            if (side == ToHitData.SIDE_FRONT) {
                // front punch hits
                switch (roll) {
                    case 1:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 2:
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 4:
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 6:
                        return new HitData(Mek.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_LEFT) {
                // left side punch hits
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_LEFT_TORSO);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_LEFT_ARM);
                    case 6:
                        return new HitData(Mek.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_RIGHT) {
                // right side punch hits
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_RIGHT_TORSO);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO);
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_ARM);
                    case 6:
                        return new HitData(Mek.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_REAR) {
                // rear punch hits
                switch (roll) {
                    case 1:
                        return new HitData(Mek.LOC_LEFT_ARM, true);
                    case 2:
                        return new HitData(Mek.LOC_LEFT_TORSO, true);
                    case 3:
                        return new HitData(Mek.LOC_CENTER_TORSO, true);
                    case 4:
                        return new HitData(Mek.LOC_RIGHT_TORSO, true);
                    case 5:
                        return new HitData(Mek.LOC_RIGHT_ARM, true);
                    case 6:
                        return new HitData(Mek.LOC_HEAD, true);
                }
            }
        }
        if (table == ToHitData.HIT_KICK) {
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

            if (playtestLocations && (side == ToHitData.SIDE_LEFT || side == ToHitData.SIDE_RIGHT)) {
                return getPlaytestSideLocation(table, side, cover);
            }

            if ((side == ToHitData.SIDE_FRONT) || (side == ToHitData.SIDE_REAR)) {
                // front/rear kick hits
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_RIGHT_LEG, (side == ToHitData.SIDE_REAR));
                    case 3:
                    case 4:
                        return new HitData(Mek.LOC_CENTER_LEG, (side == ToHitData.SIDE_REAR));
                    case 5:
                    case 6:
                        return new HitData(Mek.LOC_LEFT_LEG, (side == ToHitData.SIDE_REAR));
                }
            }
            if (side == ToHitData.SIDE_LEFT) {
                int legRoll = Compute.d6() + 1;
                if (legRoll <= 2) {
                    return new HitData(Mek.LOC_RIGHT_LEG);
                } else if (legRoll <= 4) {
                    return new HitData(Mek.LOC_CENTER_LEG);
                } else {
                    return new HitData(Mek.LOC_LEFT_LEG);
                }
            }
            if (side == ToHitData.SIDE_RIGHT) {
                int legRoll = Compute.d6() - 1;
                if (legRoll <= 2) {
                    return new HitData(Mek.LOC_RIGHT_LEG);
                } else if (legRoll <= 4) {
                    return new HitData(Mek.LOC_CENTER_LEG);
                } else {
                    return new HitData(Mek.LOC_LEFT_LEG);
                }
            }
        }
        if ((table == ToHitData.HIT_SWARM) || (table == ToHitData.HIT_SWARM_CONVENTIONAL)) {
            roll = Compute.d6(2);
            int effects;
            if (table == ToHitData.HIT_SWARM_CONVENTIONAL) {
                effects = HitData.EFFECT_NONE;
            } else {
                effects = HitData.EFFECT_CRITICAL;
            }
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
                case 2, 12:
                    return new HitData(Mek.LOC_HEAD, false, effects);
                case 3, 11:
                    return new HitData(Mek.LOC_CENTER_TORSO, true, effects);
                case 4:
                    return new HitData(Mek.LOC_RIGHT_TORSO, true, effects);
                case 5:
                    return new HitData(Mek.LOC_RIGHT_TORSO, false, effects);
                case 6:
                    return new HitData(Mek.LOC_RIGHT_ARM, false, effects);
                case 7:
                    return new HitData(Mek.LOC_CENTER_TORSO, false, effects);
                case 8:
                    return new HitData(Mek.LOC_LEFT_ARM, false, effects);
                case 9:
                    return new HitData(Mek.LOC_LEFT_TORSO, false, effects);
                case 10:
                    return new HitData(Mek.LOC_LEFT_TORSO, true, effects);
            }
        }
        if (table == ToHitData.HIT_ABOVE) {
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
            // Hits from above.
            switch (roll) {
                case 1:
                    return new HitData(Mek.LOC_LEFT_ARM, (side == ToHitData.SIDE_REAR));
                case 2:
                    return new HitData(Mek.LOC_LEFT_TORSO, (side == ToHitData.SIDE_REAR));
                case 3:
                    return new HitData(Mek.LOC_CENTER_TORSO, (side == ToHitData.SIDE_REAR));
                case 4:
                    return new HitData(Mek.LOC_RIGHT_TORSO, (side == ToHitData.SIDE_REAR));
                case 5:
                    return new HitData(Mek.LOC_RIGHT_ARM, (side == ToHitData.SIDE_REAR));
                case 6:
                    return new HitData(Mek.LOC_HEAD, (side == ToHitData.SIDE_REAR));
            }
        }
        if (table == ToHitData.HIT_BELOW) {
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
            // Hits from below.
            switch (roll) {
                case 1:
                case 2:
                case 5:
                case 6:
                    int legRoll = Compute.d6();
                    if (legRoll <= 2) {
                        return new HitData(Mek.LOC_RIGHT_LEG, side == ToHitData.SIDE_REAR);
                    } else if (legRoll <= 4) {
                        return new HitData(Mek.LOC_CENTER_LEG, side == ToHitData.SIDE_REAR);
                    } else {
                        return new HitData(Mek.LOC_LEFT_LEG, side == ToHitData.SIDE_REAR);
                    }
                case 3:
                    return new HitData(Mek.LOC_LEFT_TORSO, side == ToHitData.SIDE_REAR);
                case 4:
                    return new HitData(Mek.LOC_RIGHT_TORSO, side == ToHitData.SIDE_REAR);
            }
        }
        return null;
    }

    @Override
    public boolean isValidSecondaryFacing(int dir) {
        return canChangeSecondaryFacing();
    }

    @Override
    protected int legCount() {
        return 3;
    }
}
