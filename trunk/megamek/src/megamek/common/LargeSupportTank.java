/*
 *  MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This is a large support vehicle
 * @author beerockxs
 */
public class LargeSupportTank extends SupportTank implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -3177191060629774478L;
    // locations
    public static final int LOC_BODY = 0;
    public static final int LOC_FRONT = 1;
    public static final int LOC_FRONTRIGHT = 2;
    public static final int LOC_FRONTLEFT = 3;
    public static final int LOC_REARRIGHT = 4;
    public static final int LOC_REARLEFT = 5;
    public static final int LOC_REAR = 6;
    public static final int LOC_TURRET = 7;

    protected static String[] LOCATION_ABBRS = { "BD", "FR", "FRRS", "FRLS",
        "RRRS", "RRLS", "RR", "TU" };

    public static String[] LOCATION_NAMES = { "Body", "Front", "Front Right",
        "Front Left", "Rear Right", "Rear Left", "Rear", "Turret" };


    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Rolls up a hit location
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation,
            int aimingMode) {
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
        if(game.getOptions().booleanOption("tacops_vehicle_effective")) {
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
                rv.setEffect(HitData.EFFECT_CRITICAL);
                break;
            case 3:
                if (bSide) {
                    rv = new HitData(LOC_FRONT, false,
                            HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                } else if (bRear) {
                    rv = new HitData(LOC_REARLEFT, false,
                            HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                } else if (bRearSide) {
                    rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                } else {
                    rv = new HitData(LOC_FRONTRIGHT, false,
                            HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
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
                if ((bSide || bRearSide) && !game.getOptions().booleanOption("tacops_vehicle_effective")) {
                    rv.setEffect(HitData.EFFECT_CRITICAL);
                }
                break;
            case 9:
                if (!game.getOptions().booleanOption("tacops_vehicle_effective")) {
                    rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    rv.setMotiveMod(motiveMod);
                }
                break;
            case 10:
                if (!m_bHasNoTurret) {
                    rv = new HitData(LOC_TURRET);
                }
                break;
            case 11:
                if (!m_bHasNoTurret) {
                    rv = new HitData(LOC_TURRET);
                }
                break;
            case 12:
                if (m_bHasNoTurret) {
                    rv.setEffect(HitData.EFFECT_CRITICAL);
                } else {
                    rv = new HitData(LOC_TURRET, false, HitData.EFFECT_CRITICAL);
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
        Coords effectivePos = position;
        if (usePrior) {
            effectivePos = getPriorPosition();
        }
        if (src.equals(effectivePos)) {
            // most places handle 0 range explicitly,
            // this is a safe default (calculation gives SIDE_RIGHT)
            return ToHitData.SIDE_FRONT;
        }
        // calculate firing angle
        int fa = (effectivePos.degree(src) + (6 - face) * 60) % 360;

        int leftBetter = 2;
        // if we're right on the line, we need to special case this
        // defender would choose along which hex the LOS gets drawn, and that
        // side also determines the side we hit in
        if (fa % 30 == 0) {
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
        return m_bHasNoTurret ? 7 : 8;
    }
}
