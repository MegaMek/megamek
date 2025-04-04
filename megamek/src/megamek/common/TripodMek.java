/**
 * MegaMek - Copyright (C) 2013 Ben Mazur (bmazur@sev.org) Copyright (c) 2024, 2025 - The MegaMek Team. All Rights
 * Reserved. Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 * <p>
 * This file is part of MegaMek.
 * <p>
 * MegaMek is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * MegaMek is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with MegaMek. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import java.io.PrintWriter;
import java.io.Serial;
import java.util.List;

import megamek.common.enums.AimingMode;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

public class TripodMek extends MekWithArms {
    @Serial
    private static final long serialVersionUID = 4166375446709772785L;
    private static final MMLogger logger = MMLogger.create(TripodMek.class);

    private static final String[] LOCATION_NAMES = { "Head", "Center Torso", "Right Torso", "Left Torso", "Right Arm",
                                                     "Left Arm", "Right Leg", "Left Leg", "Center Leg" };

    private static final String[] LOCATION_ABBRS = { "HD", "CT", "RT", "LT", "RA", "LA", "RL", "LL", "CL" };

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

        setCritical(LOC_RARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_RARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_RARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_RARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));

        setCritical(LOC_LARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_LARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_LARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_LARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));

        setCritical(LOC_CLEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_CLEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_CLEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_CLEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));
    }

    @Override
    public boolean locationIsLeg(int loc) {
        return (loc == LOC_LLEG) || (loc == LOC_RLEG) || (loc == LOC_CLEG);
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
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();
        int legsDestroyed = 0;
        int hipHits = 0;
        int actuatorHits = 0;

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
            // TSM negates some heat
            if ((heat >= 9) && hasTSM(false) && legsDestroyed == 0 && movementMode != EntityMovementMode.TRACKED) {
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
        initializeInternal(leg, LOC_CLEG);
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

        for (int loc : List.of(Mek.LOC_LLEG, Mek.LOC_RLEG, Mek.LOC_CLEG)) {
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
        return weight * 3 * (150 + 80 + 120);
    }

    @Override
    public boolean hasActiveShield(int location, boolean rear) {
        switch (location) {
            case Mek.LOC_CT:
            case Mek.LOC_HEAD:
                // no rear head location so must be rear CT which is not
                // proected by
                // any shield
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
                             !isLocationBad(LOC_CLEG) &&
                             !isLocationDoomed(Mek.LOC_LLEG) &&
                             !isLocationDoomed(Mek.LOC_RLEG)) && !isLocationDoomed(LOC_CLEG)) &&
                     !isGyroDestroyed();
    }

    @Override
    public boolean cannotStandUpFromHullDown() {
        return isLocationBad(LOC_LLEG) || isLocationBad(LOC_RLEG) || isLocationBad(LOC_CLEG) || isGyroDestroyed();
    }

    @Override
    public boolean hasMPReducingHardenedArmor() {
        return (armorType[LOC_LLEG] == EquipmentType.T_ARMOR_HARDENED) ||
                     (armorType[LOC_RLEG] == EquipmentType.T_ARMOR_HARDENED) ||
                     (armorType[LOC_CLEG] == EquipmentType.T_ARMOR_HARDENED);
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
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        int roll;

        if ((aimedLocation != LOC_NONE) && !aimingMode.isNone()) {

            roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
            }
        }

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
                        } // if
                        return tac(table, side, Mek.LOC_CT, cover, false);
                    case 3:
                    case 4:
                        return new HitData(Mek.LOC_RARM);
                    case 5:
                    case 9:
                        int legRoll = Compute.d6();
                        if (legRoll <= 2) {
                            return new HitData(Mek.LOC_RLEG);
                        } else if (legRoll <= 4) {
                            return new HitData(Mek.LOC_CLEG);
                        } else {
                            return new HitData(Mek.LOC_LLEG);
                        }
                    case 6:
                        return new HitData(Mek.LOC_RT);
                    case 7:
                        return new HitData(Mek.LOC_CT);
                    case 8:
                        return new HitData(Mek.LOC_LT);
                    case 10:
                    case 11:
                        return new HitData(Mek.LOC_LARM);
                    case 12:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mek.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mek.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_LEFT) {
                // normal left side hits
                switch (roll) {
                    case 2:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_TAC) &&
                                  !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(tac(table, side, Mek.LOC_LT, cover, false));
                            return result;
                        } // if
                        return tac(table, side, Mek.LOC_LT, cover, false);
                    case 3:
                    case 6:
                    case 11:
                        int legRoll = Compute.d6() + 1;
                        if (legRoll <= 2) {
                            return new HitData(Mek.LOC_RLEG);
                        } else if (legRoll <= 4) {
                            return new HitData(Mek.LOC_CLEG);
                        } else {
                            return new HitData(Mek.LOC_LLEG);
                        }
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_LARM);
                    case 7:
                        return new HitData(Mek.LOC_LT);
                    case 8:
                        if (game.getOptions()
                                  .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_CT, true);
                        }
                        return new HitData(Mek.LOC_CT);
                    case 9:
                        if (game.getOptions()
                                  .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_RT, true);
                        }
                        return new HitData(Mek.LOC_RT);
                    case 10:
                        return new HitData(Mek.LOC_RARM);
                    case 12:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mek.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mek.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_RIGHT) {
                // normal right side hits
                switch (roll) {
                    case 2:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_TAC) &&
                                  !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(tac(table, side, Mek.LOC_RT, cover, false));
                            return result;
                        } // if
                        return tac(table, side, Mek.LOC_RT, cover, false);
                    case 3:
                    case 6:
                    case 11:
                        int legRoll = Compute.d6() - 1;
                        if (legRoll <= 2) {
                            return new HitData(Mek.LOC_RLEG);
                        } else if (legRoll <= 4) {
                            return new HitData(Mek.LOC_CLEG);
                        } else {
                            return new HitData(Mek.LOC_LLEG);
                        }
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_RARM);
                    case 7:
                        return new HitData(Mek.LOC_RT);
                    case 8:
                        if (game.getOptions()
                                  .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_CT, true);
                        }
                        return new HitData(Mek.LOC_CT);
                    case 9:
                        if (game.getOptions()
                                  .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MEK_HIT_LOCATIONS)) {
                            return new HitData(Mek.LOC_LT, true);
                        }
                        return new HitData(Mek.LOC_LT);
                    case 10:
                        return new HitData(Mek.LOC_LARM);
                    case 12:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mek.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mek.LOC_HEAD);
                }
            } else if (side == ToHitData.SIDE_REAR) {
                // normal rear hits
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ADVANCED_MEK_HIT_LOCATIONS) &&
                          isProne()) {
                    switch (roll) {
                        case 2:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_TAC) &&
                                      !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side, Mek.LOC_CT, cover, true));
                                return result;
                            } // if
                            return tac(table, side, Mek.LOC_CT, cover, true);
                        case 3:
                            return new HitData(Mek.LOC_RARM, true);
                        case 4:
                        case 5:
                        case 9:
                        case 10:
                            int legRoll = Compute.d6();
                            if (legRoll <= 2) {
                                return new HitData(Mek.LOC_RLEG);
                            } else if (legRoll <= 4) {
                                return new HitData(Mek.LOC_CLEG);
                            } else {
                                return new HitData(Mek.LOC_LLEG);
                            }
                        case 6:
                            return new HitData(Mek.LOC_RT, true);
                        case 7:
                            return new HitData(Mek.LOC_CT, true);
                        case 8:
                            return new HitData(Mek.LOC_LT, true);
                        case 11:
                            return new HitData(Mek.LOC_LARM, true);
                        case 12:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(Mek.LOC_HEAD, true));
                                return result;
                            } // if
                            return new HitData(Mek.LOC_HEAD, true);
                    }
                } else {
                    switch (roll) {
                        case 2:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_TAC) &&
                                      !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_TAC)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(tac(table, side, Mek.LOC_CT, cover, true));
                                return result;
                            } // if
                            return tac(table, side, Mek.LOC_CT, cover, true);
                        case 3:
                        case 4:
                            return new HitData(Mek.LOC_RARM, true);
                        case 5:
                        case 9:
                            int legRoll = Compute.d6();
                            if (legRoll <= 2) {
                                return new HitData(Mek.LOC_RLEG);
                            } else if (legRoll <= 4) {
                                return new HitData(Mek.LOC_CLEG);
                            } else {
                                return new HitData(Mek.LOC_LLEG);
                            }
                        case 6:
                            return new HitData(Mek.LOC_RT, true);
                        case 7:
                            return new HitData(Mek.LOC_CT, true);
                        case 8:
                            return new HitData(Mek.LOC_LT, true);
                        case 10:
                        case 11:
                            return new HitData(Mek.LOC_LARM, true);
                        case 12:
                            if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                                getCrew().decreaseEdge();
                                HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                                result.setUndoneLocation(new HitData(Mek.LOC_HEAD, true));
                                return result;
                            } // if
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
                            result.setUndoneLocation(new HitData(Mek.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mek.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_LEFT) {
                // left side punch hits
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_LT);
                    case 3:
                        return new HitData(Mek.LOC_CT);
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_LARM);
                    case 6:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mek.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mek.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_RIGHT) {
                // right side punch hits
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_RT);
                    case 3:
                        return new HitData(Mek.LOC_CT);
                    case 4:
                    case 5:
                        return new HitData(Mek.LOC_RARM);
                    case 6:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mek.LOC_HEAD));
                            return result;
                        } // if
                        return new HitData(Mek.LOC_HEAD);
                }
            }
            if (side == ToHitData.SIDE_REAR) {
                // rear punch hits
                switch (roll) {
                    case 1:
                        return new HitData(Mek.LOC_LARM, true);
                    case 2:
                        return new HitData(Mek.LOC_LT, true);
                    case 3:
                        return new HitData(Mek.LOC_CT, true);
                    case 4:
                        return new HitData(Mek.LOC_RT, true);
                    case 5:
                        return new HitData(Mek.LOC_RARM, true);
                    case 6:
                        if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                            getCrew().decreaseEdge();
                            HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                            result.setUndoneLocation(new HitData(Mek.LOC_HEAD, true));
                            return result;
                        } // if
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

            if ((side == ToHitData.SIDE_FRONT) || (side == ToHitData.SIDE_REAR)) {
                // front/rear kick hits
                switch (roll) {
                    case 1:
                    case 2:
                        return new HitData(Mek.LOC_RLEG, (side == ToHitData.SIDE_REAR));
                    case 3:
                    case 4:
                        return new HitData(Mek.LOC_CLEG, (side == ToHitData.SIDE_REAR));
                    case 5:
                    case 6:
                        return new HitData(Mek.LOC_LLEG, (side == ToHitData.SIDE_REAR));
                }
            }
            if (side == ToHitData.SIDE_LEFT) {
                int legRoll = Compute.d6() + 1;
                if (legRoll <= 2) {
                    return new HitData(Mek.LOC_RLEG);
                } else if (legRoll <= 4) {
                    return new HitData(Mek.LOC_CLEG);
                } else {
                    return new HitData(Mek.LOC_LLEG);
                }
            }
            if (side == ToHitData.SIDE_RIGHT) {
                int legRoll = Compute.d6() - 1;
                if (legRoll <= 2) {
                    return new HitData(Mek.LOC_RLEG);
                } else if (legRoll <= 4) {
                    return new HitData(Mek.LOC_CLEG);
                } else {
                    return new HitData(Mek.LOC_LLEG);
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
                case 2:
                    if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                        getCrew().decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                        result.setUndoneLocation(new HitData(Mek.LOC_HEAD, false, effects));
                        return result;
                    } // if
                    return new HitData(Mek.LOC_HEAD, false, effects);
                case 3:
                    return new HitData(Mek.LOC_CT, true, effects);
                case 4:
                    return new HitData(Mek.LOC_RT, true, effects);
                case 5:
                    return new HitData(Mek.LOC_RT, false, effects);
                case 6:
                    return new HitData(Mek.LOC_RARM, false, effects);
                case 7:
                    return new HitData(Mek.LOC_CT, false, effects);
                case 8:
                    return new HitData(Mek.LOC_LARM, false, effects);
                case 9:
                    return new HitData(Mek.LOC_LT, false, effects);
                case 10:
                    return new HitData(Mek.LOC_LT, true, effects);
                case 11:
                    return new HitData(Mek.LOC_CT, true, effects);
                case 12:
                    if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                        getCrew().decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                        result.setUndoneLocation(new HitData(Mek.LOC_HEAD, false, effects));
                        return result;
                    } // if
                    return new HitData(Mek.LOC_HEAD, false, effects);
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
                    return new HitData(Mek.LOC_LARM, (side == ToHitData.SIDE_REAR));
                case 2:
                    return new HitData(Mek.LOC_LT, (side == ToHitData.SIDE_REAR));
                case 3:
                    return new HitData(Mek.LOC_CT, (side == ToHitData.SIDE_REAR));
                case 4:
                    return new HitData(Mek.LOC_RT, (side == ToHitData.SIDE_REAR));
                case 5:
                    return new HitData(Mek.LOC_RARM, (side == ToHitData.SIDE_REAR));
                case 6:
                    if (shouldUseEdge(OptionsConstants.EDGE_WHEN_HEADHIT)) {
                        getCrew().decreaseEdge();
                        HitData result = rollHitLocation(table, side, aimedLocation, aimingMode, cover);
                        result.setUndoneLocation(new HitData(Mek.LOC_HEAD, (side == ToHitData.SIDE_REAR)));
                        return result;
                    } // if
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
                        return new HitData(Mek.LOC_RLEG, side == ToHitData.SIDE_REAR);
                    } else if (legRoll <= 4) {
                        return new HitData(Mek.LOC_CLEG, side == ToHitData.SIDE_REAR);
                    } else {
                        return new HitData(Mek.LOC_LLEG, side == ToHitData.SIDE_REAR);
                    }
                case 3:
                    return new HitData(Mek.LOC_LT, side == ToHitData.SIDE_REAR);
                case 4:
                    return new HitData(Mek.LOC_RT, side == ToHitData.SIDE_REAR);
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

    @Override
    public void clearInitiative(boolean bUseInitComp) {

    }
}
