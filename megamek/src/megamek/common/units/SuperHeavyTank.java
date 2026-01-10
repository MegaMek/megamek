/*
 * Copyright (C) 2012 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.ArrayList;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.LosEffects;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.AimingMode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;

public class SuperHeavyTank extends Tank {
    private static final MMLogger logger = MMLogger.create(SuperHeavyTank.class);

    @Serial
    private static final long serialVersionUID = 1310142644005330511L;
    // locations
    public static final int LOC_FRONT_RIGHT = 2;
    public static final int LOC_FRONT_LEFT = 3;
    public static final int LOC_REAR_RIGHT = 4;
    public static final int LOC_REAR_LEFT = 5;
    public static final int LOC_REAR = 6;
    /** for dual turret tanks, this is the rear turret **/
    public static final int LOC_TURRET = 7;
    /** for dual turret tanks, this is the front turret **/
    public static final int LOC_TURRET_2 = 8;

    // tanks have no critical slot limitations
    private static final int[] NUM_OF_SLOTS = { 25, 25, 25, 25, 25, 25, 25, 25, 25 };

    private static final String[] LOCATION_ABBREVIATIONS = { "BD", "FR", "FRRS", "FRLS", "RRRS", "RRLS", "RR", "TU",
                                                             "FT" };

    private static final String[] LOCATION_NAMES = { "Body", "Front", "Front Right", "Front Left", "Rear Right",
                                                     "Rear Left", "Rear", "Turret" };

    private static final String[] LOCATION_NAMES_DUAL_TURRET = { "Body", "Front", "Front Right", "Front Left",
                                                                 "Rear Right", "Rear Left", "Rear", "Rear Turret",
                                                                 "Front Turret" };

    @Override
    public String[] getLocationAbbreviations() {
        return LOCATION_ABBREVIATIONS;
    }

    @Override
    public String[] getLocationNames() {
        if (!hasNoDualTurret()) {
            return LOCATION_NAMES_DUAL_TURRET;
        }
        return LOCATION_NAMES;
    }

    @Override
    public int getLocTurret() {
        return LOC_TURRET;
    }

    @Override
    public int getLocTurret2() {
        return LOC_TURRET_2;
    }

    // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
    private static final TechAdvancement TA_SUPERHEAVY_TANK = new TechAdvancement(TechBase.ALL)
          .setAdvancement(2470, DATE_NONE, 3075)
          .setApproximate(true, false, true).setPrototypeFactions(Faction.LC)
          .setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return TA_SUPERHEAVY_TANK;
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
          int cover) {
        int nArmorLoc = LOC_FRONT;
        boolean bSide = false;
        boolean bRearSide = false;
        boolean bRear = false;
        int motiveMod = 0;
        if ((side == ToHitData.SIDE_FRONT) && isHullDown() && !m_bHasNoTurret) {
            // on a hull down vee, all front hits go to turret if one exists.
            nArmorLoc = LOC_TURRET;
        }
        if (side == ToHitData.SIDE_FRONT_LEFT) {
            nArmorLoc = LOC_FRONT_LEFT;
            bSide = true;
            motiveMod = 2;
        } else if (side == ToHitData.SIDE_FRONT_RIGHT) {
            nArmorLoc = LOC_FRONT_RIGHT;
            bSide = true;
            motiveMod = 2;
        } else if (side == ToHitData.SIDE_REAR_RIGHT) {
            nArmorLoc = LOC_REAR_RIGHT;
            bRearSide = true;
            motiveMod = 1;
        } else if (side == ToHitData.SIDE_REAR_LEFT) {
            nArmorLoc = LOC_REAR_LEFT;
            bRearSide = true;
            motiveMod = 1;
        } else if (side == ToHitData.SIDE_REAR) {
            nArmorLoc = LOC_REAR;
            motiveMod = 1;
            bRear = true;
        }
        if (gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_EFFECTIVE)) {
            motiveMod = 0;
        }
        HitData rv = new HitData(nArmorLoc);
        boolean bHitAimed = false;
        if ((aimedLocation != LOC_NONE) && !aimingMode.isNone()) {
            int roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                rv = new HitData(aimedLocation, side == ToHitData.SIDE_REAR,
                      true);
                bHitAimed = true;
            }
        }
        if (!bHitAimed) {
            switch (Compute.d6(2)) {
                case 2:
                    rv.setEffect(HitData.EFFECT_CRITICAL);
                    break;
                case 3:
                    if (bSide) {
                        rv = new HitData(LOC_FRONT, false);
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else if (bRear) {
                        rv = new HitData(LOC_REAR_LEFT, false);
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else if (bRearSide) {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv = new HitData(LOC_FRONT_RIGHT, false);
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
                    rv.setMotiveMod(motiveMod);
                    break;
                case 4:
                    rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    rv.setMotiveMod(motiveMod);
                    break;
                case 5:
                    if (bRear || !(bSide || bRearSide)) {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                        rv.setMotiveMod(motiveMod);
                    }
                    break;
                case 6:
                case 7:
                    break;
                case 8:
                    if ((bSide || bRearSide)
                          && !gameOptions().booleanOption(
                          OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_EFFECTIVE)) {
                        rv.setEffect(HitData.EFFECT_CRITICAL);
                    }
                    break;
                case 9:
                    if (!gameOptions()
                          .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_EFFECTIVE)) {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                        rv.setMotiveMod(motiveMod);
                    }
                    break;
                case 10:
                case 11:
                    if (!m_bHasNoTurret) {
                        if (!hasNoDualTurret()) {
                            int roll = Compute.d6();
                            if (side == ToHitData.SIDE_FRONT) {
                                roll -= 2;
                            } else if (side == ToHitData.SIDE_REAR) {
                                roll += 2;
                            }
                            if (roll <= 3) {
                                rv = new HitData(LOC_TURRET_2);
                            } else {
                                rv = new HitData(LOC_TURRET);
                            }
                        } else {
                            rv = new HitData(LOC_TURRET);
                        }
                    }
                    break;
                case 12:
                    if (!m_bHasNoTurret) {
                        if (!hasNoDualTurret()) {
                            int roll = Compute.d6();
                            if (side == ToHitData.SIDE_FRONT) {
                                roll -= 2;
                            } else if (side == ToHitData.SIDE_REAR) {
                                roll += 2;
                            }
                            if (roll <= 3) {
                                rv = new HitData(LOC_TURRET_2);
                            } else {
                                rv = new HitData(LOC_TURRET);
                            }
                        } else {
                            rv = new HitData(LOC_TURRET, false);
                        }
                    }

                    rv.setEffect(HitData.EFFECT_CRITICAL);
                    break;
            }
        }

        if (table == ToHitData.HIT_SWARM) {
            rv.setEffect(rv.getEffect() | HitData.EFFECT_CRITICAL);
        }
        return rv;
    }

    @Override
    public int sideTable(Coords src, boolean usePrior, int face) {
        Coords effectivePos = getPosition();
        if (usePrior) {
            effectivePos = getPriorPosition();
        }
        if (src.equals(effectivePos)) {
            // most places handle 0 range explicitly,
            // this is a safe default (calculation gives SIDE_RIGHT)
            return ToHitData.SIDE_FRONT;
        }
        // calculate firing angle
        int fa = (effectivePos.degree(src) + ((6 - face) * 60)) % 360;

        int leftBetter = 2;
        Board board = game.getBoard(this);
        // if we're right on the line, we need to special case this
        // defender would choose along which hex the LOS gets drawn, and that
        // side also determines the side we hit in
        if ((fa % 30) == 0) {
            Hex srcHex = board.getHex(src);
            Hex curHex = board.getHex(getPosition());
            if ((srcHex != null) && (curHex != null)) {
                LosEffects.AttackInfo ai = LosEffects.buildAttackInfo(src, getPosition(), getBoardId(),
                      1, getElevation(), srcHex.floor(), curHex.floor());
                ArrayList<Coords> in = Coords.intervening(ai.attackPos, ai.targetPos,
                      true);
                leftBetter = LosEffects.dividedLeftBetter(in, game, ai, isInBuilding(), new LosEffects());
            }
        }

        if ((fa == 330) && (leftBetter == 0)) {
            return ToHitData.SIDE_FRONT_LEFT;
        } else if ((fa == 270) && (leftBetter == 0)) {
            return ToHitData.SIDE_REAR_LEFT;
        } else if ((fa == 210) && (leftBetter == 0)) {
            return ToHitData.SIDE_REAR;
        } else if ((fa == 150) && (leftBetter == 0)) {
            return ToHitData.SIDE_REAR_RIGHT;
        } else if ((fa == 90) && (leftBetter == 1)) {
            return ToHitData.SIDE_REAR_RIGHT;
        } else if ((fa == 30) && (leftBetter == 1)) {
            return ToHitData.SIDE_FRONT_RIGHT;
        } else if ((fa > 30) && (fa <= 90)) {
            return ToHitData.SIDE_FRONT_RIGHT;
        } else if ((fa > 90) && (fa < 150)) {
            return ToHitData.SIDE_REAR_RIGHT;
        } else if ((fa >= 150) && (fa < 210)) {
            return ToHitData.SIDE_REAR;
        } else if ((fa >= 210) && (fa < 270)) {
            return ToHitData.SIDE_REAR_LEFT;
        } else if ((fa >= 270) && (fa < 330)) {
            return ToHitData.SIDE_FRONT_LEFT;
        } else {
            return ToHitData.SIDE_FRONT;
        }
    }

    @Override
    public int locations() {
        if (m_bHasNoDualTurret) {
            return m_bHasNoTurret ? 7 : 8;
        }
        return 9;
    }

    @Override
    public int height() {
        return 1;
    }

    @Override
    public boolean isSuperHeavy() {
        return true;
    }

    @Override
    public int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    @Override
    public int getWeaponArc(int weaponNumber) {
        final Mounted<?> mounted = getEquipment(weaponNumber);

        // B-Pods need to be special-cased, they have 360 firing arc
        if ((mounted.getType() instanceof WeaponType)
              && mounted.getType().hasFlag(WeaponType.F_B_POD)) {
            return Compute.ARC_360;
        }
        // VGLs base arc on their facing
        if (mounted.getType().hasFlag(WeaponType.F_VGL)) {
            return Compute.firingArcFromVGLFacing(mounted.getFacing());
        }
        switch (mounted.getLocation()) {
            case LOC_BODY:
                // Body mounted C3Ms fire into the front arc, per
                // http://forums.classicbattletech.com/index.php/topic,9400.0.html
            case LOC_FRONT:
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_FRONT;
                }
                if (gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_ARCS)) {
                    return Compute.ARC_NOSE;
                }
            case LOC_TURRET:
            case LOC_TURRET_2:
                if (gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_ARCS)) {
                    return Compute.ARC_TURRET;
                }
                return Compute.ARC_FORWARD;
            case LOC_FRONT_RIGHT:
            case LOC_REAR_RIGHT:
                if (mounted.isSponsonTurretMounted()) {
                    return Compute.ARC_SPONSON_TURRET_RIGHT;
                }
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_RIGHT;
                }
                if (gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_ARCS)) {
                    return Compute.ARC_RIGHT_BROADSIDE;
                }
                return Compute.ARC_RIGHT_SIDE;
            case LOC_FRONT_LEFT:
            case LOC_REAR_LEFT:
                if (mounted.isSponsonTurretMounted()) {
                    return Compute.ARC_SPONSON_TURRET_LEFT;
                }
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_LEFT;
                }
                if (gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_ARCS)) {
                    return Compute.ARC_LEFT_BROADSIDE;
                }
                return Compute.ARC_LEFT_SIDE;
            case LOC_REAR:
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_REAR;
                }
                if (gameOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VEHICLE_ARCS)) {
                    return Compute.ARC_AFT;
                }
                return Compute.ARC_REAR;
            default:
                return Compute.ARC_360;
        }
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        if ((getArmor(LOC_FRONT) < 1) && (getOArmor(LOC_FRONT) > 0)) {
            logger.debug("{} CRIPPLED: Front armor destroyed with Turret.", getDisplayName());
            return true;
        } else if ((getArmor(LOC_FRONT_RIGHT) < 1) && (getOArmor(LOC_FRONT_RIGHT) > 0)) {
            logger.debug("{} CRIPPLED: Front Right armor destroyed.", getDisplayName());
            return true;
        } else if ((getArmor(LOC_FRONT_LEFT) < 1) && (getOArmor(LOC_FRONT_LEFT) > 0)) {
            logger.debug("{} CRIPPLED: Front Left armor destroyed.", getDisplayName());
            return true;
        } else if ((getArmor(LOC_REAR_RIGHT) < 1) && (getOArmor(LOC_REAR_RIGHT) > 0)) {
            logger.debug("{} CRIPPLED: Rear Right armor destroyed.", getDisplayName());
            return true;
        } else if ((getArmor(LOC_REAR_LEFT) < 1) && (getOArmor(LOC_REAR_LEFT) > 0)) {
            logger.debug("{} CRIPPLED: Rear Left armor destroyed.", getDisplayName());
            return true;
        } else if (!hasNoTurret() && ((getArmor(LOC_TURRET) < 1) && (getOArmor(LOC_TURRET) > 0))) {
            logger.debug("{} CRIPPLED: Front armor destroyed.", getDisplayName());
            return true;
        } else if (!hasNoDualTurret() && ((getArmor(LOC_TURRET_2) < 1) && (getOArmor(LOC_TURRET_2) > 0))) {
            logger.debug("{} CRIPPLED: Front Turret armor destroyed.", getDisplayName());
            return true;
        } else if ((getArmor(LOC_REAR) < 1) && (getOArmor(LOC_REAR) > 0)) {
            logger.debug("{} CRIPPLED: Rear armor destroyed.", getDisplayName());
            return true;
        }

        // If this is not a military vehicle, we don't need to do a weapon check.
        if (!isMilitary()) {
            return false;
        }

        // no weapons can fire anymore, can cause no more than 5 points of combined
        // weapons damage,
        // or has no weapons with range greater than 5 hexes
        if (!hasViableWeapons()) {
            logger.debug("{} CRIPPLED: has no more viable weapons.", getDisplayName());
            return true;
        }

        return false;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_TANK | Entity.ETYPE_SUPER_HEAVY_TANK;
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(2.681 + 0.681 * Math.log(getWeight())));
    }

    @Override
    public boolean isSideLocation(int location) {
        return (location == LOC_FRONT_LEFT) || (location == LOC_FRONT_RIGHT)
              || (location == LOC_REAR_LEFT) || (location == LOC_REAR_RIGHT);
    }
}
