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

import java.io.Serial;
import java.util.List;

import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;

public class BipedMek extends MekWithArms {
    @Serial
    private static final long serialVersionUID = 4166375446709772785L;

    private static final String[] LOCATION_NAMES = { "Head", "Center Torso", "Right Torso", "Left Torso", "Right Arm",
                                                     "Left Arm", "Right Leg", "Left Leg" };

    private static final String[] LOCATION_ABBRS = { "HD", "CT", "RT", "LT", "RA", "LA", "RL", "LL" };

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

        setCritical(LOC_RARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_RARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_RARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_RARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));

        setCritical(LOC_LARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_LARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_LARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_LARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();
        int legsDestroyed = 0;
        int hipHits = 0;
        int actuatorHits = 0;

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
                mp = (legsDestroyed == 1) ? 1 : 0;
            } else {
                if (hipHits > 0) {
                    if ((game != null) &&
                              game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
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
                  (legsDestroyed == 0) &&
                  !movementMode.isTracked()) {
            if (mpCalculationSetting.forceTSM && mpCalculationSetting.ignoreHeat) {
                // When forcing TSM but ignoring heat we must assume heat to be 9 to activate TSM, this adds -1 MP!
                mp += 1;
            } else {
                mp += 2;
            }
        }

        if (!mpCalculationSetting.ignoreCargo) {
            mp = Math.max(mp - getCargoMpReduction(this), 0);
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
    public void setInternal(int head, int ct, int t, int arm, int leg) {
        initializeInternal(head, LOC_HEAD);
        initializeInternal(ct, LOC_CT);
        initializeInternal(t, LOC_RT);
        initializeInternal(t, LOC_LT);
        initializeInternal(arm, LOC_RARM);
        initializeInternal(arm, LOC_LARM);
        initializeInternal(leg, LOC_RLEG);
        initializeInternal(leg, LOC_LLEG);
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        if (hasFunctionalLegAES()) {
            roll.addModifier(-2, "AES bonus");
        }

        for (int loc : List.of(Mek.LOC_RLEG, Mek.LOC_LLEG)) {
            if (isLocationBad(loc)) {
                roll.addModifier(5, getLocationName(loc) + " destroyed");
            } else {
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) {
                    roll.addModifier(2, getLocationName(loc) + " Hip Actuator destroyed");
                    if (!game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
                        continue;
                    }
                }
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Upper Leg Actuator destroyed");
                }
                if (getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_LEG, loc) > 0) {
                    roll.addModifier(1, getLocationName(loc) + " Lower Leg Actuator destroyed");
                }
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
    protected double getLegActuatorCost() {
        return (weight * 150 * 2) + (weight * 80 * 2) + (weight * 120 * 2);
    }

    @Override
    public boolean hasActiveShield(int location, boolean rear) {

        switch (location) {
            case Mek.LOC_CT:
            case Mek.LOC_HEAD:
                // no rear head location so must be rear CT which is not proected by any shield
                if (rear) {
                    return false;
                }
                if (hasActiveShield(Mek.LOC_LARM) || hasActiveShield(Mek.LOC_RARM)) {
                    return true;
                }
                // else
                return false;
            case Mek.LOC_LARM:
            case Mek.LOC_LT:
            case Mek.LOC_LLEG:
                return hasActiveShield(Mek.LOC_LARM);
            default:
                return hasActiveShield(Mek.LOC_RARM);
        }
    }

    @Override
    public boolean canGoHullDown() {
        return (game != null) &&
                     game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN) &&
                     ((!isLocationBad(Mek.LOC_LLEG) &&
                             !isLocationBad(Mek.LOC_RLEG) &&
                             !isLocationDoomed(Mek.LOC_LLEG) &&
                             !isLocationDoomed(Mek.LOC_RLEG))) &&
                     !isGyroDestroyed();
    }

    @Override
    public boolean cannotStandUpFromHullDown() {
        return isLocationBad(LOC_LLEG) || isLocationBad(LOC_RLEG) || isGyroDestroyed();
    }

    @Override
    public boolean hasMPReducingHardenedArmor() {
        return (armorType[LOC_LLEG] == EquipmentType.T_ARMOR_HARDENED) ||
                     (armorType[LOC_RLEG] == EquipmentType.T_ARMOR_HARDENED);
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
                     hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM) &&
                     hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM) &&
                     !isLocationBad(Mek.LOC_RARM) &&
                     !isLocationBad(Mek.LOC_LARM) &&
                     !weaponFiredFrom(Mek.LOC_LARM) &&
                     !weaponFiredFrom(Mek.LOC_RARM) &&
                     !isProne();
    }

    @Override
    public void clearInitiative(boolean bUseInitComp) {

    }
}
