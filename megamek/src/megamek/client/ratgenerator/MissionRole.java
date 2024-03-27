/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.client.ratgenerator;

import java.util.Collection;
import java.util.HashSet;

import megamek.common.UnitType;

/**
 * Used to adjust availability to conform to a particular mission role.
 *
 * @author Neoancient
 */
public enum MissionRole {
    /*General combat roles */
    RECON, RAIDER, INCENDIARY, EW_SUPPORT, ARTILLERY, MISSILE_ARTILLERY, APC, TRAINING, COMMAND,
    /* Non-combat roles */
    CARGO, SUPPORT, CIVILIAN,
    /* Ground forces */
    FIRE_SUPPORT, SR_FIRE_SUPPORT, URBAN, SPOTTER, ANTI_AIRCRAFT, ANTI_INFANTRY, INF_SUPPORT, CAVALRY,
    /* Specialized ground support roles */
    SPECOPS, ENGINEER, MINESWEEPER, MINELAYER,
    /* ASF roles */
    BOMBER, ESCORT, INTERCEPTOR, GROUND_SUPPORT, //unused: STRIKE,
    /* DropShip roles */
    ASSAULT, MECH_CARRIER, ASF_CARRIER, VEE_CARRIER, INFANTRY_CARRIER, BA_CARRIER, TROOP_CARRIER,
    TUG, POCKET_WARSHIP, PROTOMECH_CARRIER,
    /* WarShip roles */
    CORVETTE, DESTROYER, FRIGATE, CRUISER, BATTLESHIP,
    /* Mechanized Battle armor */
    OMNI, MECHANIZED_BA, MAG_CLAMP,
    /* Infantry roles */
    MARINE, MOUNTAINEER, XCT, PARATROOPER, ANTI_MEK, FIELD_GUN,
    /* allows artillery but does not filter out all other roles */
    MIXED_ARTILLERY;

    public boolean fitsUnitType(int unitType) {
        switch (this) {
            case RECON:
            case SUPPORT:
                return true;

            case CIVILIAN:
                return unitType >= UnitType.SMALL_CRAFT && unitType != UnitType.WARSHIP;

            case COMMAND:
                return unitType <= UnitType.GUN_EMPLACEMENT;

            case SPECOPS:
                return unitType < UnitType.JUMPSHIP;

            case URBAN:
            case ANTI_INFANTRY:
            case INF_SUPPORT:
                return unitType <= UnitType.TANK;

            case FIRE_SUPPORT:
            case CAVALRY:
            case RAIDER:
            case APC:
                return unitType <= UnitType.TANK || unitType == UnitType.VTOL;

            case ARTILLERY:
            case MISSILE_ARTILLERY:
                return unitType < UnitType.CONV_FIGHTER || unitType == UnitType.SMALL_CRAFT || unitType == UnitType.DROPSHIP;

            case INCENDIARY:
            case ANTI_AIRCRAFT:
                return unitType <= UnitType.PROTOMEK; // all ground units

            case ENGINEER:
            case MINESWEEPER:
            case MINELAYER:
                return unitType < UnitType.TANK || unitType == UnitType.INFANTRY;

            case CARGO:
                return unitType < UnitType.BATTLE_ARMOR || unitType > UnitType.PROTOMEK;

            case EW_SUPPORT:
                return unitType <= UnitType.TANK || unitType == UnitType.AERO;

            case TRAINING:
                return unitType < UnitType.SMALL_CRAFT;

            case SPOTTER:
                return unitType <= UnitType.AERO;

            case BOMBER:
            case ESCORT:
            case INTERCEPTOR:
            case GROUND_SUPPORT:
            // case STRIKE:
                return unitType == UnitType.AERO || unitType == UnitType.CONV_FIGHTER;

            case ASSAULT:
            case VEE_CARRIER:
            case INFANTRY_CARRIER:
            case BA_CARRIER:
            case MECH_CARRIER:
            case TUG:
            case POCKET_WARSHIP:
            case PROTOMECH_CARRIER:
                return unitType == UnitType.DROPSHIP;

            case TROOP_CARRIER:
            case ASF_CARRIER:
                return unitType == UnitType.DROPSHIP || unitType == UnitType.WARSHIP;

            case CORVETTE:
            case DESTROYER:
            case FRIGATE:
            case CRUISER:
            case BATTLESHIP:
                return unitType == UnitType.WARSHIP;

            case OMNI:
                return (unitType == UnitType.MEK)
                        || (unitType == UnitType.AERO)
                        || (unitType == UnitType.TANK);
                // This should apply to fixed-wing support also, but that cannot be distinguished from conventional fighters here.

            case MECHANIZED_BA:
            case MAG_CLAMP:
                return unitType == UnitType.BATTLE_ARMOR;

            case MARINE:
            case XCT:
                return unitType == UnitType.INFANTRY || unitType == UnitType.BATTLE_ARMOR;

            case MOUNTAINEER:
            case PARATROOPER:
            case ANTI_MEK:
            case FIELD_GUN:
                return unitType == UnitType.INFANTRY;

            case MIXED_ARTILLERY:
                /* TODO: allow inclusion of artillery without binary either/or */
            default:
                return false;
        }
    }

    public static Double adjustAvailabilityByRole(double avRating,
                                                  Collection<MissionRole> desiredRoles,
                                                  ModelRecord mRec, int year, int strictness) {
        boolean roleApplied = false;
        if (desiredRoles == null) {
            desiredRoles = new HashSet<>();
        }
        double[] avAdj = new double[5];
        for (int i = 0; i < avAdj.length; i++) {
            avAdj[i] = (i + 1) * strictness / 3.0;
        }

        if (!desiredRoles.isEmpty()) {
            roleApplied = true;
            for (MissionRole role : desiredRoles) {
                switch (role) {
                    case ARTILLERY:
                        if (!mRec.getRoles().contains(ARTILLERY)
                                && !mRec.getRoles().contains(MISSILE_ARTILLERY)) {
                            return null;
                        }
                        break;
                    case MISSILE_ARTILLERY:
                        if (!mRec.getRoles().contains(MISSILE_ARTILLERY)) {
                            return null;
                        }
                        break;
                    case ENGINEER:
                        if (!mRec.getRoles().contains(ENGINEER)) {
                            return null;
                        }
                        break;
                    case RECON:
                        if (mRec.getRoles().contains(RECON)) {
                            avRating += avAdj[2];
                        } else if (mRec.getRoles().contains(EW_SUPPORT)) {
                            avRating += avAdj[1];
                        } else if (mRec.getRoles().contains(SPOTTER)) {
                            avRating += avAdj[0];
                        } else {
                            if (isSpecialized(desiredRoles, mRec)) {
                                return null;
                            } else if (mRec.getUnitType() != UnitType.INFANTRY
                                    && mRec.getUnitType() != UnitType.BATTLE_ARMOR
                                        && mRec.getSpeed() < 4 + avAdj[2] - mRec.getWeightClass()) {
                                return null;
                            } else {
                                avRating += mRec.getSpeed() - (4 + avAdj[2] - mRec.getWeightClass());
                                if (mRec.getRoles().contains(URBAN) ||
                                        mRec.getRoles().contains(INF_SUPPORT) ||
                                        mRec.getRoles().contains(ANTI_INFANTRY) ||
                                        mRec.getRoles().contains(APC) ||
                                        mRec.getRoles().contains(MOUNTAINEER) ||
                                        mRec.getRoles().contains(PARATROOPER) ||
                                        mRec.getRoles().contains(MARINE) ||
                                        mRec.getRoles().contains(XCT)) {
                                    avRating -= avAdj[3];
                                } else if (mRec.getRoles().contains(SPECOPS)) {
                                    avRating -= avAdj[4];
                                } else {
                                    avRating -= avAdj[0];
                                }
                            }
                        }
                        break;
                    case COMMAND:
                        if (mRec.getRoles().contains(COMMAND)) {
                            avRating += avAdj[2];
                        }
                        if ((ModelRecord.NETWORK_COMPANY_COMMAND & mRec.getNetworkMask()) != 0) {
                            avRating += avAdj[1];
                        } else if ((ModelRecord.NETWORK_C3_MASTER & mRec.getNetworkMask()) != 0) {
                            avRating += avAdj[0];
                        }
                        break;
                    case FIELD_GUN:
                        if (!mRec.getRoles().contains(FIELD_GUN)) {
                            return null;
                        }
                        break;
                    case FIRE_SUPPORT:
                        if (mRec.getRoles().contains(FIRE_SUPPORT)
                                || mRec.getLongRange() > 0.75) {
                            avRating += avAdj[2];
                        } else if (mRec.getLongRange() > 0.5) {
                            avRating += avAdj[1];
                        } else if (mRec.getLongRange() > 0.2) {
                            avRating += avAdj[0];
                        } else if (mRec.getRoles().contains(SR_FIRE_SUPPORT)
                                || mRec.getRoles().contains(ANTI_AIRCRAFT)
                                || mRec.getRoles().contains(MISSILE_ARTILLERY)) {
                            avRating += avAdj[0];
                        } else {
                            return null;
                        }
                        break;
                    case INF_SUPPORT:
                        if (mRec.getRoles().contains(INF_SUPPORT)) {
                            avRating += strictness;
                        } else if (mRec.getRoles().contains(APC)) {
                            avRating += avAdj[1];
                        } else if (mRec.getRoles().contains(ANTI_INFANTRY)
                                || mRec.getRoles().contains(URBAN)) {
                            avRating += avAdj[0];
                        } else {
                            if (isSpecialized(desiredRoles, mRec)) {
                                return null;
                            } else {
                                if (mRec.getRoles().contains(MOUNTAINEER) ||
                                        mRec.getRoles().contains(PARATROOPER)) {
                                    avRating -= avAdj[2];
                                } else if (mRec.getRoles().contains(INCENDIARY) ||
                                        mRec.getRoles().contains(MARINE) ||
                                        mRec.getRoles().contains(XCT)) {
                                    avRating -= avAdj[3];
                                } else if (mRec.getRoles().contains(SPECOPS)) {
                                    avRating -= avAdj[4];
                                } else {
                                    avRating -= avAdj[1];
                                }
                            }
                        }
                        break;
                    case OMNI:
                        if (isSpecialized(desiredRoles, mRec) || !mRec.isOmni()) {
                            return null;
                        }
                        break;
                    case MECHANIZED_BA:
                        if (mRec.getUnitType() == UnitType.BATTLE_ARMOR
                                && !mRec.canDoMechanizedBA()) {
                            return null;
                        }
                        break;
                    case MAG_CLAMP:
                        if (mRec.getUnitType() == UnitType.BATTLE_ARMOR
                                && !mRec.hasMagClamp()) {
                            return null;
                        }
                        break;
                    case URBAN:
                        if (mRec.getRoles().contains(URBAN)) {
                            avRating += avAdj[2];
                        } else if (mRec.getRoles().contains(ANTI_INFANTRY)) {
                            avRating += avAdj[1];
                        } else if (mRec.getRoles().contains(INF_SUPPORT)) {
                            avRating += avAdj[0];
                        } else {
                            if (isSpecialized(desiredRoles, mRec)) {
                                return null;
                            }
                            if (mRec.getRoles().contains(FIRE_SUPPORT) ||
                                    mRec.getRoles().contains(SR_FIRE_SUPPORT) ||
                                    mRec.getRoles().contains(ANTI_AIRCRAFT)) {
                                avRating -= avAdj[0];
                            }
                            if (mRec.getRoles().contains(MARINE) ||
                                    mRec.getRoles().contains(XCT)) {
                                avRating -= avAdj[2];
                            }
                            if (mRec.getRoles().contains(MOUNTAINEER) ||
                                    mRec.getRoles().contains(PARATROOPER) ||
                                    mRec.getRoles().contains(SPECOPS)) {
                                avRating -= avAdj[3];
                            }
                        }
                        if (avRating > 0 && mRec.getUnitType() == UnitType.TANK) {
                            if (mRec.getMechSummary().getUnitSubType().equals("Wheeled")) {
                                avRating += avAdj[2];
                            } else if (mRec.getMechSummary().getUnitSubType().equals("Tracked")) {
                                avRating -= avAdj[2];
                            }
                        }
                        break;
                    case RAIDER:
                        if (mRec.getRoles().contains(RAIDER)) {
                            avRating += avAdj[2];
                        } else {
                            if (isSpecialized(desiredRoles, mRec)) {
                                return null;
                            } else if (mRec.getAmmoRequirement() < 0.2) {
                                avRating += avAdj[0];
                            } else if (mRec.getAmmoRequirement() < 0.5) {
                                avRating += avAdj[1];
                            }
                            if (mRec.getUnitType() != UnitType.INFANTRY
                                    && mRec.getUnitType() != UnitType.BATTLE_ARMOR
                                        && mRec.getSpeed() < 3 + avAdj[2] - mRec.getWeightClass()) {
                                avRating -= 2 + avAdj[2] - mRec.getWeightClass() - mRec.getSpeed();
                            }
                        }
                        break;
                    case INCENDIARY:
                        if (mRec.getRoles().contains(INCENDIARY)) {
                            avRating += avAdj[2];
                        } else {
                            if (isSpecialized(desiredRoles, mRec)) {
                                avRating = 0;
                            } else if (mRec.hasIncendiaryWeapon()) {
                                avRating += avAdj[2];
                            } else {
                                avRating -= avAdj[2];
                            }
                        }
                        break;
                    case ANTI_AIRCRAFT:
                        if (mRec.getRoles().contains(ANTI_AIRCRAFT) ||
                                mRec.getFlak() > 0.75) {
                            avRating += avAdj[2];
                        } else if (mRec.getFlak() > 0.5) {
                            avRating += avAdj[1];
                        } else if (mRec.getFlak() > 0.2) {
                            avRating += avAdj[0];
                        } else {
                            if (isSpecialized(desiredRoles, mRec)) {
                                return null;
                            } else {
                                avRating -= avAdj[1];
                            }
                        }
                        break;
                    case ANTI_INFANTRY:
                        if (mRec.getRoles().contains(ANTI_INFANTRY)) {
                            avRating += avAdj[2];
                        } else     if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        } else if (!mRec.hasAPWeapons()) {
                            avRating -= avAdj[2];
                        }
                        break;

                    case GROUND_SUPPORT:
                        if (mRec.getRoles().contains(GROUND_SUPPORT)) {
                            avRating += avAdj[2];
                        } else {
                            avRating -= avAdj[2];
                        }
                        break;
                    case INTERCEPTOR:
                        if (mRec.getRoles().contains(INTERCEPTOR)) {
                            avRating += avAdj[2];
                        } else {
                            avRating -= avAdj[2];
                        }
                        break;
                    case BOMBER:
                        if (mRec.getRoles().contains(BOMBER)) {
                            avRating += avAdj[2];
                        } else {
                            avRating -= avAdj[2];
                        }
                        break;
                    case ESCORT:
                        if (mRec.getRoles().contains(ESCORT)) {
                            avRating += avAdj[2];
                        } else {
                            avRating -= avAdj[2];
                        }
                        break;
                    case CAVALRY:
                        if (mRec.getUnitType() == UnitType.INFANTRY
                                || mRec.getUnitType() == UnitType.BATTLE_ARMOR) {
                            avRating += avAdj[2] * (mRec.getSpeed() - 3);
                        } else {
                            avRating += avAdj[2] * (mRec.getSpeed() - (7 - mRec.getWeightClass()));
                        }
                        break;
                    // Large craft roles
                    case CIVILIAN:
                        if (!mRec.getRoles().contains(CIVILIAN)) {
                            return null;
                        }
                        break;
                    case CARGO:
                        if (mRec.getRoles().contains(CARGO)) {
                            avRating += avAdj[2];
                        } else if (mRec.getRoles().contains(CIVILIAN)) {
                            avRating += avAdj[1];
                        } else {
                            avRating -= avAdj[2];
                        }
                        break;
                    case SUPPORT:
                        if (mRec.getRoles().contains(SUPPORT)) {
                            avRating += avAdj[2];
                        } else if (mRec.getRoles().contains(TUG)) {
                            avRating += avAdj[1];
                        } else if (mRec.getRoles().contains(CARGO)
                                    || mRec.getRoles().contains(CIVILIAN)) {
                            avRating += avAdj[0];
                        } else {
                            avRating -= avAdj[2];
                        }
                        break;
                    case TUG:
                        if (!mRec.getRoles().contains(TUG)) {
                            return null;
                        }
                        break;
                    case POCKET_WARSHIP:
                        if (mRec.getRoles().contains(POCKET_WARSHIP)) {
                            avRating += avAdj[2];
                        } else if (mRec.getRoles().contains(ASSAULT)) {
                            avRating += avAdj[0];
                        } else {
                            return null;
                        }
                        break;
                    case ASSAULT:
                        if (mRec.getRoles().contains(ASSAULT)) {
                            avRating += avAdj[2];
                        } else if (mRec.getRoles().contains(POCKET_WARSHIP)) {
                            avRating += avAdj[0];
                        } else {
                            return null;
                        }
                        break;
                    case MECH_CARRIER:
                        if (mRec.getRoles().contains(MECH_CARRIER)) {
                            avRating += avAdj[2];
                        } else if (mRec.getRoles().contains(TROOP_CARRIER)) {
                            avRating += avAdj[0];
                        } else {
                            return null;
                        }
                        break;
                    case ASF_CARRIER:
                        if (mRec.getRoles().contains(ASF_CARRIER)) {
                            avRating += avAdj[2];
                        } else if (mRec.getRoles().contains(ASSAULT)) {
                            avRating += avAdj[0];
                        } else {
                            return null;
                        }
                        break;
                    case BA_CARRIER:
                        if (!mRec.getRoles().contains(BA_CARRIER)) {
                            return null;
                        }
                        break;
                    case INFANTRY_CARRIER:
                        if (mRec.getRoles().contains(INFANTRY_CARRIER)) {
                            avRating += avAdj[2];
                        } else if (mRec.getRoles().contains(TROOP_CARRIER)) {
                            avRating += avAdj[0];
                        } else {
                            return null;
                        }
                        break;
                    case VEE_CARRIER:
                        if (mRec.getRoles().contains(VEE_CARRIER)) {
                            avRating += avAdj[2];
                        } else if (mRec.getRoles().contains(TROOP_CARRIER)) {
                            avRating += avAdj[0];
                        } else {
                            return null;
                        }
                        break;
                    case PROTOMECH_CARRIER:
                        if (!mRec.getRoles().contains(PROTOMECH_CARRIER)) {
                            return null;
                        }
                        break;
                    case TROOP_CARRIER:
                        if (!mRec.getRoles().contains(TROOP_CARRIER)) {
                            return null;
                        }
                        break;
                    case CORVETTE:
                        if (!mRec.getRoles().contains(CORVETTE)) {
                            return null;
                        }
                        break;
                    case DESTROYER:
                        if (!mRec.getRoles().contains(DESTROYER)) {
                            return null;
                        }
                        break;
                    case FRIGATE:
                        if (!mRec.getRoles().contains(FRIGATE)) {
                            return null;
                        }
                        break;
                    case CRUISER:
                        if (!mRec.getRoles().contains(CRUISER)) {
                            return null;
                        }
                        break;
                    case BATTLESHIP:
                        if (!mRec.getRoles().contains(BATTLESHIP)) {
                            return null;
                        }
                        break;
                    default:
                        roleApplied = false;
                }
            }
        }
        /* Reduce rating of units with certain specialized functions if the roles does not
         * require them.
         */
        if (!roleApplied) {
            if ((mRec.getRoles().contains(SUPPORT) && !desiredRoles.contains(SUPPORT)) ||
                    (mRec.getRoles().contains(CARGO) && !(desiredRoles.contains(CARGO) || desiredRoles.size() > 1 || mRec.getUnitType() == UnitType.WARSHIP)) ||
                    (mRec.getRoles().contains(TUG) && !desiredRoles.contains(TUG)) ||
                    (mRec.getRoles().contains(CIVILIAN) && !desiredRoles.contains(CIVILIAN)) ||
                    (mRec.getRoles().contains(TRAINING) && !desiredRoles.contains(TRAINING)) ||
                    (mRec.getRoles().contains(FIELD_GUN) && !desiredRoles.contains(FIELD_GUN)) ||
                    (mRec.getRoles().contains(ARTILLERY) && !desiredRoles.contains(MIXED_ARTILLERY))) {
                return null;
            }
            if (mRec.getRoles().contains(MISSILE_ARTILLERY) &&
                    !desiredRoles.contains(ARTILLERY)
                    && !desiredRoles.contains(MISSILE_ARTILLERY)
                    && !desiredRoles.contains(FIRE_SUPPORT)
                    && !desiredRoles.contains(MIXED_ARTILLERY)) {
                return null;
            }
            if (mRec.getRoles().contains(RECON) ||
                    mRec.getRoles().contains(EW_SUPPORT)) {
                avRating -= avAdj[0];
            }
            if (mRec.getRoles().contains(URBAN) ||
                    mRec.getRoles().contains(INF_SUPPORT) ||
                    mRec.getRoles().contains(ANTI_INFANTRY) ||
                    mRec.getRoles().contains(APC) ||
                    mRec.getRoles().contains(MOUNTAINEER) ||
                    mRec.getRoles().contains(PARATROOPER)) {
                avRating -= avAdj[1];
            }
            if (mRec.getRoles().contains(INCENDIARY) ||
                    mRec.getRoles().contains(MARINE) ||
                    mRec.getRoles().contains(XCT)) {
                avRating -= avAdj[2];
            }
            if (mRec.getRoles().contains(SPECOPS)) {
                avRating -= avAdj[3];
            }
        }
        return avRating;
    }

    private static boolean isSpecialized(Collection<MissionRole> desiredRoles,
            ModelRecord mRec) {
        return (mRec.getRoles().contains(SUPPORT) && !desiredRoles.contains(SUPPORT)) ||
                (mRec.getRoles().contains(CARGO) && !desiredRoles.contains(CARGO)) ||
                (mRec.getRoles().contains(TUG) && !desiredRoles.contains(TUG)) ||
                (mRec.getRoles().contains(CIVILIAN) && !desiredRoles.contains(CIVILIAN)) ||
                (mRec.getRoles().contains(TRAINING) && !desiredRoles.contains(TRAINING)) ||
                (mRec.getRoles().contains(ARTILLERY) && !desiredRoles.contains(ARTILLERY)
                        && !desiredRoles.contains(MIXED_ARTILLERY));
    }

    public static MissionRole parseRole(String role) {
        switch (role.toLowerCase().replace("_", " ")) {
            case "recon":
                return RECON;
            case "fire support":
                return FIRE_SUPPORT;
            case "command":
                return COMMAND;
            case "sr fire support":
                return SR_FIRE_SUPPORT;
            case "spotter":
                return SPOTTER;
            case "urban":
                return URBAN;
            case "infantry support":
            case "inf support":
                return INF_SUPPORT;
            case "cavalry":
                return CAVALRY;
            case "raider":
                return RAIDER;
            case "incendiary":
                return INCENDIARY;
            case "ew support":
                return EW_SUPPORT;
            case "artillery":
                return ARTILLERY;
            case "missile artillery":
                return MISSILE_ARTILLERY;
            case "anti-aircraft":
            case "anti aircraft":
                return ANTI_AIRCRAFT;
            case "anti-infantry":
            case "anti infantry":
                return ANTI_INFANTRY;
            case "apc":
                return APC;
            case "spec ops":
            case "specops":
                return SPECOPS;
            case "cargo":
                return CARGO;
            case "support":
                return SUPPORT;
            case "bomber":
                return BOMBER;
            case "escort":
                return ESCORT;
            case "interceptor":
                return INTERCEPTOR;
            case "ground support":
                return GROUND_SUPPORT;
            // case "strike":
                //return STRIKE;
            case "training":
                return TRAINING;
            case "assault":
                return ASSAULT;
            case "mech carrier":
                return MECH_CARRIER;
            case "asf carrier":
                return ASF_CARRIER;
            case "vee carrier":
                return VEE_CARRIER;
            case "infantry carrier":
                return INFANTRY_CARRIER;
            case "ba carrier":
                return BA_CARRIER;
            case "protomech carrier":
                return PROTOMECH_CARRIER;
            case "tug":
                return TUG;
            case "troop carrier":
                return TROOP_CARRIER;
            case "pocket ws":
            case "pocket warship":
                return POCKET_WARSHIP;
            case "corvette":
                return CORVETTE;
            case "destroyer":
                return DESTROYER;
            case "frigate":
                return FRIGATE;
            case "cruiser":
                return CRUISER;
            case "battleship":
                return BATTLESHIP;
            case "engineer":
                return ENGINEER;
            case "marine":
                return MARINE;
            case "mountaineer":
                return MOUNTAINEER;
            case "xct":
                return XCT;
            case "paratrooper":
                return PARATROOPER;
            case "anti-mek":
            case "anti mek":
                return ANTI_MEK;
            case "omni":
                return OMNI;
            case "mechanized ba":
                return MECHANIZED_BA;
            case "mag clamp":
                return MAG_CLAMP;
            case "field gun":
                return FIELD_GUN;
            case "civilian":
                return CIVILIAN;
            case "minesweeper":
                return MINESWEEPER;
            case "minelayer":
                return MINELAYER;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
