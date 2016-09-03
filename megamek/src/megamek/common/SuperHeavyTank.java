/*
 *  MegaMek - Copyright (C) 2012 Ben Mazur (bmazur@sev.org)
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

import java.util.ArrayList;

import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;

public class SuperHeavyTank extends Tank {

    /**
     *
     */
    private static final long serialVersionUID = 1310142644005330511L;
    // locations
    public static final int LOC_FRONTRIGHT = 2;
    public static final int LOC_FRONTLEFT = 3;
    public static final int LOC_REARRIGHT = 4;
    public static final int LOC_REARLEFT = 5;
    public static final int LOC_REAR = 6;
    /** for dual turret tanks, this is the rear turret **/
    public static final int LOC_TURRET = 7;
    /** for dual turret tanks, this is the front turret **/
    public static final int LOC_TURRET_2 = 8;

    // tanks have no critical slot limitations
    private static final int[] NUM_OF_SLOTS =
        { 25, 25, 25, 25, 25, 25, 25, 25, 25 };

    private static String[] LOCATION_ABBRS = { "BD", "FR", "FRRS", "FRLS",
        "RRRS", "RRLS", "RR", "TU", "FT" };

    private static String[] LOCATION_NAMES = { "Body", "Front", "Front Right",
        "Front Left", "Rear Right", "Rear Left", "Rear", "Turret" };

    private static String[] LOCATION_NAMES_DUAL_TURRET = { "Body", "Front", "Front Right",
        "Front Left", "Rear Right", "Rear Left", "Rear", "Rear Turret", "Front Turret" };

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
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

    /**
     * Rolls up a hit location
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation,
            int aimingMode, int cover) {
        int nArmorLoc = LOC_FRONT;
        boolean bSide = false;
        boolean bRearSide = false;
        boolean bRear = false;
        int motiveMod = 0;
        if ((side == ToHitData.SIDE_FRONT) && isHullDown() && !m_bHasNoTurret) {
            // on a hull down vee, all front hits go to turret if one exists.
            nArmorLoc = LOC_TURRET;
        }
        if (side == ToHitData.SIDE_FRONTLEFT) {
            nArmorLoc = LOC_FRONTLEFT;
            bSide = true;
            motiveMod = 2;
        } else if (side == ToHitData.SIDE_FRONTRIGHT) {
            nArmorLoc = LOC_FRONTRIGHT;
            bSide = true;
            motiveMod = 2;
        } else if (side == ToHitData.SIDE_REARRIGHT) {
            nArmorLoc = LOC_REARRIGHT;
            bRearSide = true;
            motiveMod = 1;
        } else if (side == ToHitData.SIDE_REARLEFT) {
            nArmorLoc = LOC_REARLEFT;
            bRearSide = true;
            motiveMod = 1;
        } else if (side == ToHitData.SIDE_REAR) {
            nArmorLoc = LOC_REAR;
            motiveMod = 1;
            bRear = true;
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_EFFECTIVE)) {
            motiveMod = 0;
        }
        HitData rv = new HitData(nArmorLoc);
        boolean bHitAimed = false;
        if ((aimedLocation != LOC_NONE)
                && (aimingMode != IAimingModes.AIM_MODE_NONE)) {

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
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                    setPotCrit(HitData.EFFECT_CRITICAL);
                } else {
                    rv.setEffect(HitData.EFFECT_CRITICAL);
                }
                break;
            case 3:
                if (bSide) {
                    rv = new HitData(LOC_FRONT, false);
                    if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
                } else if (bRear) {
                    rv = new HitData(LOC_REARLEFT, false);
                    if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
                } else if (bRearSide) {
                    if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
                } else {
                    rv = new HitData(LOC_FRONTRIGHT, false);
                    if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
                }
                rv.setMotiveMod(motiveMod);
                break;
            case 4:
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                    setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                } else {
                    rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                }
                rv.setMotiveMod(motiveMod);
                break;
            case 5:
                if (bRear || !(bSide || bRearSide)) {
                    if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
                    rv.setMotiveMod(motiveMod);
                }
                break;
            case 6:
            case 7:
                break;
            case 8:
                if ((bSide || bRearSide)
                        && !game.getOptions().booleanOption(
                                OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_EFFECTIVE)) {
                    if (game.getOptions().booleanOption(
                            OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_CRITICAL);
                    } else {
                        rv.setEffect(HitData.EFFECT_CRITICAL);
                    }
                }
                break;
            case 9:
                if (!game.getOptions()
                        .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_EFFECTIVE)) {
                    if (game.getOptions().booleanOption(
                            OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
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
                if (m_bHasNoTurret) {
                    if (game.getOptions().booleanOption(
                            OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_CRITICAL);
                    } else {
                        rv.setEffect(HitData.EFFECT_CRITICAL);
                    }
                } else {
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
                    if (game.getOptions().booleanOption(
                            OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
                        setPotCrit(HitData.EFFECT_CRITICAL);
                    } else {
                        rv.setEffect(HitData.EFFECT_CRITICAL);
                    }
                }
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
        // if we're right on the line, we need to special case this
        // defender would choose along which hex the LOS gets drawn, and that
        // side also determines the side we hit in
        if ((fa % 30) == 0) {
            IHex srcHex = game.getBoard().getHex(src);
            IHex curHex = game.getBoard().getHex(getPosition());
            if ((srcHex != null) && (curHex != null)) {
                LosEffects.AttackInfo ai = LosEffects.buildAttackInfo(src, getPosition(),
                        1, getElevation(), srcHex.floor(), curHex.floor());
                ArrayList<Coords> in = Coords.intervening(ai.attackPos, ai.targetPos,
                        true);
                leftBetter = LosEffects.dividedLeftBetter(in, game, ai,
                        Compute.isInBuilding(game, this), new LosEffects());
            }
        }

        if ((fa == 330) && (leftBetter == 0)) {
            return ToHitData.SIDE_FRONTLEFT;
        } else if ((fa == 270) && (leftBetter == 0)) {
            return ToHitData.SIDE_REARLEFT;
        } else if ((fa == 210) && (leftBetter == 0)) {
            return  ToHitData.SIDE_REAR;
        } else if ((fa == 150) && (leftBetter == 0)) {
            return ToHitData.SIDE_REARRIGHT;
        } else if ((fa == 90) && (leftBetter == 1)) {
            return ToHitData.SIDE_REARRIGHT;
        } else if ((fa == 30) && (leftBetter == 1)) {
            return ToHitData.SIDE_FRONTRIGHT;
        } else if ((fa > 30) && (fa <= 90)) {
            return ToHitData.SIDE_FRONTRIGHT;
        } else if ((fa > 90) && (fa < 150)) {
            return ToHitData.SIDE_REARRIGHT;
        } else if ((fa >= 150) && (fa < 210)) {
            return ToHitData.SIDE_REAR;
        } else if ((fa >= 210) && (fa < 270)) {
            return ToHitData.SIDE_REARLEFT;
        } else if ((fa >= 270) && (fa < 330)) {
            return ToHitData.SIDE_FRONTLEFT;
        } else {
            return ToHitData.SIDE_FRONT;
        }
    }

    /**
     * Returns the number of locations in the entity
     */
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

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);

        // B-Pods need to be special-cased, the have 360 firing arc
        if ((mounted.getType() instanceof WeaponType) 
                && mounted.getType().hasFlag(WeaponType.F_B_POD)) {
            return Compute.ARC_360;
        }
        // VGLs always be considered forward, since arc is set by VGL facing
        if (mounted.getType().hasFlag(WeaponType.F_VGL)) {
            return Compute.ARC_FORWARD;
        }
        switch (mounted.getLocation()) {
            case LOC_BODY:
                // Body mounted C3Ms fire into the front arc,
                // per
                // http://forums.classicbattletech.com/index.php/topic,9400.0.html
            case LOC_FRONT:
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_FRONT;
                }
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                    return Compute.ARC_NOSE;
                }
            case LOC_TURRET:
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                    return Compute.ARC_TURRET;
                }
                return Compute.ARC_FORWARD;
            case LOC_TURRET_2:
                return Compute.ARC_FORWARD;
            case LOC_FRONTRIGHT:
            case LOC_REARRIGHT:
                if (mounted.isSponsonTurretMounted()) {
                    return Compute.ARC_SPONSON_TURRET_RIGHT;
                }
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_RIGHT;
                }
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                    return Compute.ARC_RIGHT_BROADSIDE;
                }
                return Compute.ARC_RIGHTSIDE;
            case LOC_FRONTLEFT:
            case LOC_REARLEFT:
                if (mounted.isSponsonTurretMounted()) {
                    return Compute.ARC_SPONSON_TURRET_LEFT;
                }
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_LEFT;
                }
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                    return Compute.ARC_LEFT_BROADSIDE;
                }
                return Compute.ARC_LEFTSIDE;
            case LOC_REAR:
                if (mounted.isPintleTurretMounted()) {
                    return Compute.ARC_PINTLE_TURRET_REAR;
                }
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                    return Compute.ARC_AFT;
                }
                return Compute.ARC_REAR;
            default:
                return Compute.ARC_360;
        }
    }

    @Override
    public boolean isCrippled() {
        if (getArmor(LOC_FRONT) < 1 && getOArmor(LOC_FRONT) > 0) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Front armor destroyed.");
            }
            return true;
        }
        if (getArmor(LOC_FRONTRIGHT) < 1 && getOArmor(LOC_FRONTRIGHT) > 0) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Front Right armor destroyed.");
            }
            return true;
        }
        if (getArmor(LOC_FRONTLEFT) < 1 && getOArmor(LOC_FRONTLEFT) > 0) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Front Left armor destroyed.");
            }
            return true;
        }
        if (getArmor(LOC_REARRIGHT) < 1 && getOArmor(LOC_REARRIGHT) > 0) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Rear Right armor destroyed.");
            }
            return true;
        }
        if (getArmor(LOC_REARLEFT) < 1 && getOArmor(LOC_REARLEFT) > 0) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Rear Left armor destroyed.");
            }
            return true;
        }
        if (!hasNoTurret() && (getArmor(LOC_TURRET) < 1 && getOArmor(LOC_TURRET) > 0)) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Turret armor destroyed.");
            }
            return true;
        }
        if (!hasNoDualTurret() && (getArmor(LOC_TURRET_2) < 1 && getOArmor(LOC_TURRET_2) > 0)) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Front Turret armor destroyed.");
            }
            return true;
        }
        if (getArmor(LOC_REAR) < 1 && getOArmor(LOC_REAR) > 0) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Rear armor destroyed.");
            }
            return true;
        }
        
        if (isPermanentlyImmobilized(true)) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out
                        .println(getDisplayName() + " CRIPPLED: Immobilized.");
            }
            return true;
        }

        // If this is not a military vehicle, we don't need to do a weapon
        // check.
        if (!isMilitary()) {
            return false;
        }

        // no weapons can fire anymore, can cause no more than 5 points of
        // combined weapons damage,
        // or has no weapons with range greater than 5 hexes
        if (!hasViableWeapons()) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: has no more viable weapons.");
            }
            return true;
        }
        return false;
    }

    @Override
    public long getEntityType(){
        return Entity.ETYPE_TANK | Entity.ETYPE_SUPER_HEAVY_TANK;
    }

}
