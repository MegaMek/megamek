/*
  Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org), Cord Awtry (kipsta@bs-interactive.com)
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

package megamek.common.units;

import java.io.Serial;
import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.MPCalculationSetting;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.PilotingRollData;

public class BipedMek extends MekWithArms {
    @Serial
    private static final long serialVersionUID = 4166375446709772785L;

    private static final String[] LOCATION_NAMES = { "Head", "Center Torso", "Right Torso", "Left Torso", "Right Arm",
                                                     "Left Arm", "Right Leg", "Left Leg" };

    private static final String[] LOCATION_ABBREVIATIONS = { "HD", "CT", "RT", "LT", "RA", "LA", "RL", "LL" };

    private static final int[] NUM_OF_SLOTS = { 6, 12, 12, 12, 12, 12, 6, 6 };

    public BipedMek(String inGyroType, String inCockpitType) {
        this(getGyroTypeForString(inGyroType), getCockpitTypeForString(inCockpitType));
    }

    public BipedMek() {
        this(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD);
    }

    public BipedMek(int inGyroType, int inCockpitType) {
        super(inGyroType, inCockpitType);

        movementMode = EntityMovementMode.BIPED;
        originalMovementMode = EntityMovementMode.BIPED;

        setCritical(LOC_RIGHT_ARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_RIGHT_ARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_RIGHT_ARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_RIGHT_ARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));

        setCritical(LOC_LEFT_ARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_LEFT_ARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_LEFT_ARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_LEFT_ARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));
    }

    // PLAYTEST2 New Method for immobile due to no legs.
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
        int leftLegActuators = 0;
        int rightLegActuators = 0;

        //A Mek using tracks has its movement reduced by 50% per leg or track destroyed;
        if (getMovementMode().isTracked()) {
            for (Mounted<?> m : getMisc()) {
                if (m.getType().hasFlag(MiscType.F_TRACKS)) {
                    if (m.isHit() || isLocationBad(m.getLocation())) {
                        legsDestroyed++;
                    }
                }
            }
            mp = (mp * (2 - legsDestroyed)) / 2;
        } else {
            for (int i : List.of(Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_LEG)) {
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
                            hipHits++;
                            if ((game == null) ||
                                  !gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE)) {
                                continue;
                            }
                        }
                        if (i == Mek.LOC_LEFT_LEG) {
                            leftLegActuators += countLegActuatorCrits(i);
                        }
                        if (i == Mek.LOC_RIGHT_LEG) {
                            rightLegActuators += countLegActuatorCrits(i);
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
                                  !gameOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEG_DAMAGE)) {
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
                    int maxReduction;
                    minReduction = (int) Math.ceil(mp / 2.0);
                    maxReduction = (int) Math.ceil(minReduction / 2.0);

                    if (hipHits == 1 || legsDestroyed == 1) {
                        // Both a hip and a leg
                        if (hipHits == 1 && legsDestroyed == 1) {
                            mp = mp - minReduction - maxReduction;
                        } else {
                            // Only a single hip or leg
                            mp = mp - minReduction;
                        }
                    } else if (hipHits == 2) {
                        // Can only happen if both legs exist
                        mp = mp - minReduction - maxReduction;
                    }
                    if (leftHip == 0) {
                        mp -= leftLegActuators;
                    }
                    if (rightHip == 0) {
                        mp -= rightLegActuators;
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
        }

        // TSM negates some heat, but provides no benefit when using tracks.
        if (((heat >= 9) || mpCalculationSetting.forceTSM()) &&
              hasTSM(false) &&
              (legsDestroyed == 0) &&
              !movementMode.isTracked()) {
            if (mpCalculationSetting.forceTSM() && mpCalculationSetting.ignoreHeat()) {
                // When forcing TSM but ignoring heat we must assume heat to be 9 to activate TSM, this adds -1 MP!
                mp += 1;
            } else {
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
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        if (hasFunctionalLegAES()) {
            roll.addModifier(-2, "AES bonus");
        }

        for (int loc : List.of(Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_LEG)) {
            // PLAYTEST2 destroyed leg
            if (isLocationBad(loc)) {
                if (gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                    roll.addModifier(4, getLocationName(loc) + " destroyed");
                } else {
                    roll.addModifier(5, getLocationName(loc) + " destroyed");
                }
            } else {
                if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) {
                    // PLAYTEST2 now +1
                    if (gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                        roll.addModifier(1, getLocationName(loc) + " Hip Actuator destroyed");
                    } else {
                        roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                    }
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
                // PLAYTEST2 foot actuator no longer +1
                if (!gameOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                    if (getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_FOOT, loc) > 0) {
                        roll.addModifier(1, getLocationName(loc) + " Foot Actuator destroyed");
                    }
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
        return (weight * 150 * 2) + (weight * 80 * 2) + (weight * 120 * 2);
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
                    !isLocationDoomed(Mek.LOC_LEFT_LEG) &&
                    !isLocationDoomed(Mek.LOC_RIGHT_LEG))) &&
              !isGyroDestroyed();
    }

    @Override
    public boolean cannotStandUpFromHullDown() {
        return isLocationBad(LOC_LEFT_LEG) || isLocationBad(LOC_RIGHT_LEG) || isGyroDestroyed();
    }

    @Override
    public boolean hasMPReducingHardenedArmor() {
        return (armorType[LOC_LEFT_LEG] == EquipmentType.T_ARMOR_HARDENED) ||
              (armorType[LOC_RIGHT_LEG] == EquipmentType.T_ARMOR_HARDENED);
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_MEK | Entity.ETYPE_BIPED_MEK;
    }

    /**
     * @return True if this unit is capable of Zweihandering (weapon attack using both hands)
     */
    public boolean canZweihander() {
        return (getCrew() != null) &&
              hasAbility(OptionsConstants.PILOT_ZWEIHANDER) &&
              hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM) &&
              hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM) &&
              !isLocationBad(Mek.LOC_RIGHT_ARM) &&
              !isLocationBad(Mek.LOC_LEFT_ARM) &&
              !weaponFiredFrom(Mek.LOC_LEFT_ARM) &&
              !weaponFiredFrom(Mek.LOC_RIGHT_ARM) &&
              !isProne();
    }
}
