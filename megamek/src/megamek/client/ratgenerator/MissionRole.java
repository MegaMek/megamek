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

import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
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
    BOMBER, ESCORT, INTERCEPTOR, GROUND_SUPPORT,
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

            // RECON applies to all unit types except gun emplacements, JumpShips,
            // space stations, and some specialized aerospace
            case RECON:
                return unitType != UnitType.GUN_EMPLACEMENT &&
                        unitType != UnitType.JUMPSHIP &&
                        unitType != UnitType.SPACE_STATION &&
                        unitType != UnitType.AERO;

            // EW_SUPPORT role applies to all ground units, VTOL, blue water naval, gun emplacement,
            // and small craft. Infantry and large spacecraft are excluded.
            case EW_SUPPORT:
                return unitType == UnitType.MEK ||
                        unitType == UnitType.TANK ||
                        unitType == UnitType.PROTOMEK ||
                        unitType == UnitType.VTOL ||
                        unitType == UnitType.NAVAL ||
                        unitType == UnitType.GUN_EMPLACEMENT ||
                        unitType == UnitType.SMALL_CRAFT;

            // SPOTTER role applies to all ground units plus VTOL, blue water naval, gun
            // emplacements, and fixed wing aircraft.
            case SPOTTER:
                return unitType <= UnitType.AEROSPACEFIGHTER;

            // COMMAND role applies to all ground units, VTOLs, blue water naval, conventional
            // fixed wing aircraft, and small craft. Conventional infantry, battle armor,
            // ProtoMechs, gun emplacements, and large space vessels are excluded.
            case COMMAND:
                return unitType == UnitType.MEK ||
                        unitType == UnitType.TANK ||
                        unitType == UnitType.VTOL ||
                        unitType == UnitType.NAVAL ||
                        unitType == UnitType.CONV_FIGHTER ||
                        unitType == UnitType.SMALL_CRAFT;

            // Fire support roles apply to most types except fixed wing aircraft and spacecraft
            case FIRE_SUPPORT:
            case SR_FIRE_SUPPORT:
                return unitType <= UnitType.GUN_EMPLACEMENT;

            // Artillery roles apply to all ground units, VTOL, blue water naval, gun emplacements,
            // and conventional fighters. Small craft and DropShips, which are capable of mounting
            // artillery type weapons, are included.  ProtoMechs cannot carry any existing artillery
            // weapons so are excluded.
            case ARTILLERY:
            case MISSILE_ARTILLERY:
            case MIXED_ARTILLERY:
                return unitType <= UnitType.INFANTRY ||
                        unitType == UnitType.VTOL ||
                        unitType == UnitType.NAVAL ||
                        unitType == UnitType.GUN_EMPLACEMENT ||
                        unitType == UnitType.CONV_FIGHTER ||
                        unitType == UnitType.SMALL_CRAFT ||
                        unitType == UnitType.DROPSHIP;

            // URBAN role applies to all ground units. Although infantry are inherently
            // urban-oriented this role should be reserved for mechanized (wheeled) and others which
            // are not optimized for non-urban terrain.
            case URBAN:
                return unitType <= UnitType.PROTOMEK;

            // Infantry support roles are limited to ground units and VTOLs. This includes infantry
            // and battle armor that are armed primarily with anti-infantry weapons.
            case ANTI_INFANTRY:
            case INF_SUPPORT:
                return unitType <= UnitType.PROTOMEK ||
                        unitType == UnitType.VTOL;

            // APC role is limited to units which can carry conventional infantry. Although blue
            // water naval units can carry infantry they have limited use so are excluded.
            case APC:
                return unitType == UnitType.TANK ||
                        unitType == UnitType.VTOL;

            // Naturally limited to battle armor
            case MECHANIZED_BA:
                return unitType == UnitType.BATTLE_ARMOR;

            // Both battle armor and ProtoMechs can make use of mag clamps
            case MAG_CLAMP:
                return unitType == UnitType.BATTLE_ARMOR ||
                        unitType == UnitType.PROTOMEK;

            // MARINE role applies to select battle armor and conventional infantry units equipped
            // for space combat
            case MARINE:
                return unitType == UnitType.BATTLE_ARMOR ||
                        unitType == UnitType.INFANTRY;

            // Conventional infantry roles:
            //    PARATROOPER may be added on non-foot infantry to designate 'airmobile'
            case MOUNTAINEER:
            case PARATROOPER:
            case ANTI_MEK:
            case FIELD_GUN:
            case XCT:
                return unitType == UnitType.INFANTRY;

            // CAVALRY applies to Mechs, ground vehicles, and ProtoMechs
            case CAVALRY:
                return unitType == UnitType.MEK ||
                        unitType == UnitType.TANK ||
                        unitType == UnitType.PROTOMEK;

            // RAIDER can be applied to Mechs, ground vehicles, ProtoMechs, and VTOLs
            case RAIDER:
                return  unitType == UnitType.MEK ||
                        unitType == UnitType.TANK ||
                        unitType == UnitType.PROTOMEK ||
                        unitType == UnitType.VTOL;

            // ANTI_AIRCRAFT role applies to all ground units, plus blue water naval and
            // gun emplacements. Conventional infantry are included (field guns/artillery) but
            // not battle armor or ProtoMechs.
            case ANTI_AIRCRAFT:
                return unitType == UnitType.MEK ||
                        unitType == UnitType.TANK ||
                        unitType == UnitType.INFANTRY ||
                        unitType == UnitType.NAVAL ||
                        unitType == UnitType.GUN_EMPLACEMENT;

            // INCENDIARY applies to all ground units. This excludes VTOL, blue water naval,
            // and gun emplacements.
            case INCENDIARY:
                return unitType <= UnitType.PROTOMEK;

            // SPECOPS role applies to Mechs, ground vehicles, VTOLs, conventional infantry, and
            // battle armor.
            case SPECOPS:
                return unitType == UnitType.MEK ||
                        unitType == UnitType.TANK ||
                        unitType == UnitType.INFANTRY ||
                        unitType == UnitType.BATTLE_ARMOR ||
                        unitType == UnitType.VTOL;

            // OMNI applies to all units which are capable of being built to make use of pod-mounted
            // equipment.  This is primarily used to determine suitability for mechanized battle
            // armor but other uses may be added later.
            case OMNI:
                return unitType == UnitType.MEK ||
                        unitType == UnitType.TANK ||
                        unitType == UnitType.AEROSPACEFIGHTER;

            // Roles for conventional and aerospace fighters
            case BOMBER:
            case INTERCEPTOR:
                return unitType == UnitType.CONV_FIGHTER ||
                        unitType == UnitType.AEROSPACEFIGHTER;

            case GROUND_SUPPORT:
                return unitType == UnitType.CONV_FIGHTER ||
                        unitType == UnitType.AEROSPACEFIGHTER ||
                        unitType == UnitType.SMALL_CRAFT;

            case ESCORT:
                return  unitType == UnitType.CONV_FIGHTER ||
                        unitType == UnitType.AEROSPACEFIGHTER ||
                        unitType == UnitType.SMALL_CRAFT;

            // Roles for DropShips
            case ASSAULT:
            case INFANTRY_CARRIER:
            case BA_CARRIER:
                return unitType == UnitType.DROPSHIP ||
                        unitType == UnitType.SMALL_CRAFT;

            case VEE_CARRIER:
            case MECH_CARRIER:
            case TUG:
            case POCKET_WARSHIP:
            case PROTOMECH_CARRIER:
                return unitType == UnitType.DROPSHIP;

            // Mixed units carrier and aerospace carrier roles apply to DropShips and
            // WarShips
            case TROOP_CARRIER:
            case ASF_CARRIER:
                return unitType == UnitType.DROPSHIP || unitType == UnitType.WARSHIP;

            // Roles for WarShips are primarily 'class' designations.
            case CORVETTE:
            case DESTROYER:
            case FRIGATE:
            case CRUISER:
            case BATTLESHIP:
                return unitType == UnitType.WARSHIP;

            // TRAINING applies to Mechs, ground vehicles, VTOLs, blue water naval, and conventional
            // fighters. Infantry, battle armor, ProtoMechs, and gun emplacements are excluded.
            case TRAINING:
                return unitType == UnitType.MEK ||
                        unitType == UnitType.TANK ||
                        unitType == UnitType.VTOL ||
                        unitType == UnitType.NAVAL ||
                        unitType == UnitType.CONV_FIGHTER;

            // ENGINEER applies to Mechs, ground vehicles, and conventional infantry
            case ENGINEER:
                return unitType == UnitType.MEK ||
                        unitType == UnitType.TANK ||
                        unitType == UnitType.INFANTRY;

            // MINESWEEPER and MINELAYER roles apply to ground vehicles, battle armor, and
            // conventional infantry
            case MINESWEEPER:
            case MINELAYER:
                return unitType == UnitType.TANK ||
                        unitType == UnitType.BATTLE_ARMOR ||
                        unitType == UnitType.INFANTRY;

            // SUPPORT applies to all non-combat ground units, VTOLs, blue water naval, conventional
            // fighters, small craft, and some specialized aerospace. ProtoMechs, gun
            // emplacements, and WarShips are excluded as these are strictly combat units.
            case SUPPORT:
                return unitType != UnitType.PROTOMEK &&
                        unitType != UnitType.GUN_EMPLACEMENT &&
                        unitType != UnitType.WARSHIP;

            // CIVILIAN applies to all non-combat vehicles in civilian service, which includes
            // all ground units, VTOLs, blue water naval, conventional fighters, small craft,
            // DropShips, JumpShips, space stations, and some specialized aerospace. ProtoMechs,
            // gun emplacements, aerospace fighters, and WarShips are excluded as they are strictly
            // combat units.
            case CIVILIAN:
                return unitType != UnitType.PROTOMEK &&
                        unitType != UnitType.GUN_EMPLACEMENT &&
                        unitType != UnitType.AEROSPACEFIGHTER &&
                        unitType != UnitType.WARSHIP;

            // CARGO applies to ground vehicles, VTOLs, blue water naval, conventional fighters,
            // small craft, and all large space vessels.
            case CARGO:
                return unitType == UnitType.TANK ||
                        unitType == UnitType.VTOL ||
                        unitType == UnitType.NAVAL ||
                        unitType == UnitType.CONV_FIGHTER ||
                        (unitType >= UnitType.SMALL_CRAFT && unitType <= UnitType.SPACE_STATION);

            default:
                return false;
        }
    }

    /**
     * Adjusts the provided availability rating based on desired roles, the roles the provided
     * unit has, and other factors such as unit type, movement speed, and weapon types.
     * @param avRating     Availability rating as positive number, typically 0-10
     * @param desiredRoles Roles that are desired or mandatory, use null for general use
     * @param mRec         ModelRecord of specific unit to check
     * @param year         Year to test in
     * @param strictness   Zero or higher, larger values are more restrictive
     * @return             Modified avRating, higher if better suited for a specific role, lower if
     *                     less suited, or null if unit is not compatible
     */
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

        double min_adjust = avAdj[0];
        double light_adjust = avAdj[1];
        double medium_adjust = avAdj[2];
        double strong_adjust = avAdj[3];
        double max_adjust = avAdj[4];

        // If specific roles are desired, iterate through them. Certain cases where role
        // requirements are highly restrictive may result in early exit.
        if (!desiredRoles.isEmpty()) {
            roleApplied = true;

            for (MissionRole role : desiredRoles) {

                switch (role) {

                    // Calling for recon units will exclude non-combat and civilian units.
                    // EW_SUPPORT, SPECOPS, and SPOTTER roles ARE included at a lower priority.
                    // Additional fast VTOL, hovercraft, and light Mech/ProtoMech units may also
                    // be included if they do not explicitly have the role. All other units are
                    // excluded.
                    case RECON:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(RECON)) {
                            avRating += medium_adjust;
                        } else if (mRec.getRoles().contains(EW_SUPPORT) ||
                                mRec.getRoles().contains(SPECOPS)) {
                            avRating += light_adjust;
                        } else if (mRec.getRoles().contains(SPOTTER)) {
                            avRating += min_adjust;
                        } else if ((mRec.getUnitType() == UnitType.MEK ||
                                mRec.getUnitType() == UnitType.PROTOMEK) &&
                                (mRec.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT ||
                                        mRec.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) &&
                                (mRec.getSpeed() >= 6)) {
                            avRating = Math.max(avRating-strong_adjust, 1.0);
                        } else if (mRec.getUnitType() == UnitType.VTOL ||
                                mRec.getMovementMode() == EntityMovementMode.HOVER) {
                            avRating = Math.max(avRating-strong_adjust, 1.0);
                        } else {
                            return null;
                        }
                        break;

                    // Calling for EW_SUPPORT (electronic warfare/support) units only return units
                    // which include the role
                    case EW_SUPPORT:
                        if (!mRec.getRoles().contains(EW_SUPPORT) ||
                                isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        break;

                    // Calling for SPOTTER returns units with TAG for precision guided weapons. Other
                    // units, primarily infantry and battle armor, may be included at a lower
                    // priority especially in eras where TAG does not exist.
                    case SPOTTER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(SPOTTER)) {
                            avRating += strong_adjust;
                        } else if (mRec.getUnitType() == UnitType.INFANTRY ||
                                        mRec.getUnitType() == UnitType.BATTLE_ARMOR) {
                            avRating -= medium_adjust;
                        } else if (mRec.getRoles().contains(RECON)) {
                            avRating -= medium_adjust;
                        } else {
                            avRating -= strong_adjust;
                        }
                        break;

                    // COMMAND units may be specialized in other roles. Units with C3 master
                    // equipment take priority, while others without the role or equipment are
                    // reduced in priority.
                    case COMMAND:
                        if (mRec.getRoles().contains(CIVILIAN) &&
                                !desiredRoles.contains(CIVILIAN)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(COMMAND)) {
                            avRating += medium_adjust;
                        }
                        if ((ModelRecord.NETWORK_COMPANY_COMMAND & mRec.getNetworkMask()) != 0) {
                            avRating += light_adjust;
                        } else if ((ModelRecord.NETWORK_C3_MASTER & mRec.getNetworkMask()) != 0) {
                            avRating += min_adjust;
                        } else {
                            avRating -= strong_adjust;
                        }
                        break;

                    // Calling for FIRE_SUPPORT prioritizes units with a significant percentage of
                    // their equipment BV as long range weapons, or includes artillery. Units that
                    // do not meet these requirements are excluded.
                    case FIRE_SUPPORT:
                        if (mRec.getRoles().contains(SUPPORT) ||
                                mRec.getRoles().contains(CIVILIAN)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(FIRE_SUPPORT) || mRec.getLongRange() > 0.75) {
                            avRating += medium_adjust;
                        } else if (mRec.getLongRange() > 0.5) {
                            avRating += light_adjust;
                        } else if (mRec.getLongRange() > 0.2) {
                            avRating += min_adjust;
                        } else if (mRec.getRoles().contains(ARTILLERY) ||
                                mRec.getRoles().contains(MISSILE_ARTILLERY) ||
                                mRec.getRoles().contains(MIXED_ARTILLERY)) {
                            avRating += min_adjust;
                        } else {
                            return null;
                        }
                        break;

                    // Calling for SR_FIRE_SUPPORT prioritizes units that lack significant long
                    // range weapons and artillery.
                    case SR_FIRE_SUPPORT:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(SR_FIRE_SUPPORT)) {
                            avRating += medium_adjust;
                        } else if (!mRec.getRoles().contains(FIRE_SUPPORT) &&
                                mRec.getLongRange() <= 0.2) {
                            avRating += light_adjust;
                        } else if (mRec.getLongRange() >= 0.5) {
                            return null;
                        }
                        break;

                    // Calling for ARTILLERY includes all units with artillery, including missile
                    // artillery. Units without artillery are excluded.
                    case ARTILLERY:
                        if (mRec.getRoles().contains(CIVILIAN) &&
                                !desiredRoles.contains(CIVILIAN)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(ARTILLERY) &&
                                !mRec.getRoles().contains(MISSILE_ARTILLERY) &&
                                !mRec.getRoles().contains(MIXED_ARTILLERY)) {
                            return null;
                        }
                        break;

                    // Calling for MIXED_ARTILLERY only includes units which have the role. Other
                    // units with artillery are considered specific-purpose, while these are
                    // 'mixed use' i.e. both combat and artillery.
                    case MIXED_ARTILLERY:
                        if (mRec.getRoles().contains(CIVILIAN) &&
                                !desiredRoles.contains(CIVILIAN)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(MIXED_ARTILLERY)) {
                            return null;
                        }
                        break;

                    // Calling for missile artillery in specific, will exclude units which do not
                    // have this specific role.
                    case MISSILE_ARTILLERY:
                        if (!mRec.getRoles().contains(MISSILE_ARTILLERY)) {
                            return null;
                        }
                        break;

                    // Calling for URBAN includes units that specialize in supporting or fighting
                    // against conventional infantry, are focused on short range firepower, or
                    // use wheeled propulsion. Units that specialize in long range firepower
                    // are reduced priority.
                    case URBAN:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(URBAN)) {
                            avRating += strong_adjust;
                        } else if (mRec.getRoles().contains(ANTI_INFANTRY) ||
                                mRec.getRoles().contains(SR_FIRE_SUPPORT) ||
                                mRec.getLongRange() <= 0.2) {
                            avRating += light_adjust;
                        } else if (mRec.getRoles().contains(INF_SUPPORT)) {
                            avRating += min_adjust;
                        } else {
                            if (mRec.getRoles().contains(FIRE_SUPPORT) ||
                                    mRec.getLongRange() >= 0.5) {
                                avRating -= min_adjust;
                            } else {
                                avRating -= medium_adjust;
                            }
                        }
                        if (mRec.getMovementMode() == EntityMovementMode.WHEELED) {
                            avRating += medium_adjust;
                        } else if (mRec.getMovementMode() == EntityMovementMode.TRACKED ||
                                mRec.getMovementMode() == EntityMovementMode.HOVER) {
                            avRating -= medium_adjust;
                        }
                        break;

                    // Calling for ANTI_INFANTRY includes units without the role which have
                    // anti-personnel weapons. Other units have reduced priority.
                    case ANTI_INFANTRY:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(ANTI_INFANTRY)) {
                            avRating += medium_adjust;
                        } else if (mRec.hasAPWeapons()) {
                            avRating += light_adjust;
                        } else {
                            avRating -= strong_adjust;
                        }
                        break;

                    // Calling for INF_SUPPORT may include units with the APC, FIRE_SUPPORT, or
                    // ANTI_AIRCRAFT role at a lower priority.  Units with artillery may also be
                    // included at lower priority.
                    case INF_SUPPORT:
                        if (mRec.getRoles().contains(SUPPORT) ||
                                mRec.getRoles().contains(CIVILIAN)) {
                            return null;
                        } else if (mRec.getRoles().contains(INF_SUPPORT)) {
                            avRating += medium_adjust;
                        } else if (mRec.getRoles().contains(FIRE_SUPPORT) ||
                                mRec.getRoles().contains(ANTI_AIRCRAFT) ||
                                mRec.getRoles().contains(MIXED_ARTILLERY)) {
                            avRating -= min_adjust;
                        } else if (mRec.getRoles().contains(APC) ||
                                mRec.getRoles().contains(ANTI_AIRCRAFT) ||
                                mRec.getRoles().contains(ARTILLERY)) {
                            avRating -= light_adjust;
                        } else {
                            avRating -= max_adjust;
                        }
                        break;

                    // Calling for armored personnel carriers should only return units which have
                    // that role
                    case APC:
                        if (mRec.getRoles().contains(SUPPORT) &&
                                !desiredRoles.contains(SUPPORT)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(CIVILIAN) &&
                                !desiredRoles.contains(CIVILIAN)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(APC)) {
                            return null;
                        }
                        break;

                    // Calling for MECHANIZED_BA should only return units which can either ride on
                    // omni-based transport or use mag-clamps
                    case MECHANIZED_BA:
                        if (mRec.getRoles().contains(SUPPORT) &&
                                !desiredRoles.contains(SUPPORT)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(CIVILIAN) &&
                                !desiredRoles.contains(CIVILIAN)) {
                            return null;
                        }
                        if (!mRec.canDoMechanizedBA() &&
                                !mRec.getRoles().contains(MAG_CLAMP)) {
                            return null;
                        }
                        break;

                    // Calling for MAG_CLAMP should only return units equipped with
                    // mag-clamp equipment, which includes ProtoMechs
                    case MAG_CLAMP:
                        if (mRec.getRoles().contains(CIVILIAN) &&
                                !desiredRoles.contains(CIVILIAN)) {
                            return null;
                        }
                        if (!mRec.hasMagClamp() && !mRec.getRoles().contains(MAG_CLAMP)) {
                            return null;
                        }
                        break;

                    // Calling for MARINE only returns infantry or battle armor which is equipped
                    // for space combat. All other units are excluded.
                    case MARINE:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(MARINE)) {
                            return null;
                        }
                        break;

                    // Calling for MOUNTAINEER only returns units which are equipped as such. All
                    // other units are excluded.
                    case MOUNTAINEER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(MOUNTAINEER)) {
                            return null;
                        }
                        break;

                    // Calling for PARATROOPER only returns units which can be air dropped. Under
                    // most circumstances this will be foot infantry with the paratrooper
                    // specialization, but may include jump infantry at a lower priority. Other
                    // movement types may be considered 'airmobile' but will require the specific
                    // role.
                    case PARATROOPER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(PARATROOPER)) {
                            avRating += strong_adjust;
                        } else {
                            if (mRec.getMovementMode() == EntityMovementMode.INF_JUMP) {
                                avRating = Math.max(avRating - medium_adjust, 1.0);
                            } else {
                                return null;
                            }
                        }
                        break;

                    // Calling for ANTI_MEK assumes infantry with the role have the appropriate
                    // equipment. Other non-mechanized infantry may be included at a lower priority.
                    // Infantry that cannot conduct anti-Mech attacks should be excluded.
                    case ANTI_MEK:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(ANTI_MEK)) {
                            avRating += strong_adjust;
                        } else if (mRec.getMovementMode() == EntityMovementMode.INF_LEG ||
                                mRec.getMovementMode() == EntityMovementMode.INF_JUMP ||
                                mRec.getMovementMode() == EntityMovementMode.INF_MOTORIZED) {
                            avRating -= medium_adjust;
                        } else {
                            return null;
                        }
                        break;

                    // Calling for FIELD_GUN infantry excludes units without the role
                    case FIELD_GUN:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(FIELD_GUN)) {
                            return null;
                        }
                        break;

                    // Calling for XCT returns units equipped to operate in hostile atmospheres
                    // and weather conditions. Marines are included at a lower priority. All other
                    // units are excluded.
                    case XCT:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(XCT)) {
                            avRating += medium_adjust;
                        } else if (mRec.getRoles().contains(MARINE)) {
                            avRating = Math.max(avRating - light_adjust, 1.0);
                        } else {
                            return null;
                        }
                        break;

                    // Calling for CAVALRY includes fast ground vehicle, and medium or heavy
                    // Mechs/ProtoMechs, with a lower priority. All other units are excluded.
                    case CAVALRY:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(CAVALRY)) {
                            avRating += medium_adjust;
                        } else if ((mRec.getUnitType() == UnitType.MEK ||
                                mRec.getUnitType() == UnitType.PROTOMEK)) {
                            if (mRec.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM &&
                                    mRec.getSpeed() >= 6) {
                                avRating -= Math.max(avRating - strong_adjust, 1.0);
                            } else if (mRec.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY &&
                                    mRec.getSpeed() >= 5) {
                                avRating = Math.max(avRating - strong_adjust, 1.0);
                            } else if (mRec.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT &&
                                    mRec.getSpeed() >=5 && mRec.getSpeed() <= 7) {
                                avRating = Math.max(avRating - strong_adjust, 1.0);
                            } else {
                                return null;
                            }
                        } else if (mRec.getMovementMode() == EntityMovementMode.HOVER &&
                                mRec.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                            avRating = Math.max(avRating - strong_adjust, 1.0);
                        } else if (mRec.getMovementMode() == EntityMovementMode.TRACKED &&
                                mRec.getSpeed() >= 5) {
                            if (mRec.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT &&
                                    mRec.getSpeed() <= 6) {
                                avRating = Math.max(avRating - strong_adjust, 1.0);
                            } else if (mRec.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                                avRating = Math.max(avRating - strong_adjust, 1.0);
                            } else {
                                return null;
                            }
                        } else {
                            return null;
                        }
                        break;

                    // Calling for RAIDER includes faster and ammo-independent light, medium, or
                    // heavy Mechs/ProtoMechs with a lower priority. All other units are excluded.
                    case RAIDER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(RAIDER)) {
                            avRating += medium_adjust;
                        } else {
                            if (mRec.getUnitType() == UnitType.MEK ||
                                    mRec.getUnitType() == UnitType.PROTOMEK) {
                                if ((mRec.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT &&
                                        mRec.getSpeed() >= 5) ||
                                        (mRec.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM &&
                                                mRec.getSpeed() >= 5) ||
                                        (mRec.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY &&
                                                mRec.getSpeed() >= 4)) {
                                    if (mRec.getAmmoRequirement() < 0.2) {
                                        avRating = Math.max(avRating - medium_adjust, 1.0);
                                    } else if (mRec.getAmmoRequirement() < 0.5) {
                                        avRating = Math.max(avRating - strong_adjust, 1.0);
                                    }
                                } else {
                                    return null;
                                }
                            } else {
                                return null;
                            }
                        }
                        break;

                    // Calling for ANTI_AIRCRAFT includes units with a high percentage of
                    // flak-based weapons. Additional units with FIRE_SUPPORT or artillery may be
                    // included at a lower priority. Other units are excluded.
                    case ANTI_AIRCRAFT:
                        if (mRec.getRoles().contains(SUPPORT) ||
                                mRec.getRoles().contains(CIVILIAN)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(ANTI_AIRCRAFT) ||
                                mRec.getFlak() > 0.75) {
                            avRating += medium_adjust;
                        } else if (mRec.getFlak() > 0.5) {
                            avRating += light_adjust;
                        } else if (mRec.getFlak() > 0.2) {
                            avRating += min_adjust;
                        } else if (mRec.getRoles().contains(ARTILLERY) ||
                                mRec.getRoles().contains(MISSILE_ARTILLERY)) {
                            avRating = Math.max(avRating - medium_adjust, 1.0);
                        } else if (mRec.getUnitType() != UnitType.INFANTRY &&
                                mRec.getRoles().contains(FIRE_SUPPORT)) {
                            avRating = Math.max(avRating - strong_adjust, 1.0);
                        } else {
                            return null;
                        }
                        break;

                    // Calling for INCENDIARY includes units with incendiary weapons at a lower
                    // priority. Other units will be excluded.
                    case INCENDIARY:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(INCENDIARY)) {
                            avRating += medium_adjust;
                        } else {
                            if (mRec.hasIncendiaryWeapon()) {
                                avRating -= light_adjust;
                            } else {
                                return null;
                            }
                        }
                        break;

                    // Calling for SPECOPS may return additional units with the RECON or RAIDER
                    // role at a lower priority. General units may be included at a lower priority.
                    case SPECOPS:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(SPECOPS)) {
                            avRating += strong_adjust;
                        } else if (mRec.getRoles().contains(RECON) ||
                                mRec.getRoles().contains(RAIDER)) {
                            avRating -= light_adjust;
                        } else if (mRec.getUnitType() != UnitType.INFANTRY) {
                            avRating -= medium_adjust;
                        } else {
                            avRating -= strong_adjust;
                        }
                        break;

                    // Calling for OMNI will only return units with that characteristic. Unlike
                    // other roles, this is pulled from the unit data rather than a role tag.
                    case OMNI:
                        if (mRec.getRoles().contains(SUPPORT) &&
                                !desiredRoles.contains(SUPPORT)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(CIVILIAN) &&
                                !desiredRoles.contains(CIVILIAN)) {
                            return null;
                        }
                        if (isSpecialized(desiredRoles, mRec) || !mRec.isOmni()) {
                            return null;
                        }
                        break;

                    // Calling for TRAINING will return other units without the role at a lower
                    // priority
                    case TRAINING:
                        if (mRec.getRoles().contains(SUPPORT) &&
                                !desiredRoles.contains(SUPPORT)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(CIVILIAN) &&
                                !desiredRoles.contains(CIVILIAN)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(TRAINING)) {
                            avRating += strong_adjust;
                        } else {
                            avRating -= medium_adjust;
                        }
                        break;

                    // Calling for GROUND_SUPPORT includes other units at a lower priority
                    case GROUND_SUPPORT:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(GROUND_SUPPORT)) {
                            avRating += medium_adjust;
                        } else if (mRec.getRoles().contains(BOMBER)) {
                            avRating -= light_adjust;
                        } else {
                            avRating -= medium_adjust;
                        }
                        break;

                    // Calling for INTERCEPTOR includes other units at a lower priority
                    case INTERCEPTOR:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(INTERCEPTOR)) {
                            avRating += medium_adjust;
                        } else {
                            avRating -= medium_adjust;
                        }
                        break;

                    // Calling for BOMBER includes other units at a lower priority
                    case BOMBER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(BOMBER)) {
                            avRating += medium_adjust;
                        } else {
                            avRating -= medium_adjust;
                        }
                        break;

                    // Calling for ESCORT includes other units at a lower priority
                    case ESCORT:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(ESCORT)) {
                            avRating += medium_adjust;
                        } else {
                            avRating -= medium_adjust;
                        }
                        break;

                    // Calling for ASSAULT (assault DropShip) may include pocket warships at a
                    // lower priority. Other units are excluded.
                    case ASSAULT:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(ASSAULT)) {
                            avRating += medium_adjust;
                        } else if (mRec.getRoles().contains(POCKET_WARSHIP)) {
                            avRating -= min_adjust;
                        } else {
                            return null;
                        }
                        break;

                    // Calling for VEE_CARRIER (vehicle transport) may include troop (multi-type)
                    // transports at a lower priority. Other units are excluded.
                    case VEE_CARRIER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(VEE_CARRIER)) {
                            avRating += medium_adjust;
                        } else if (mRec.getRoles().contains(TROOP_CARRIER)) {
                            avRating -= min_adjust;
                        } else {
                            return null;
                        }
                        break;

                    // Calling for INFANTRY_CARRIER may include troop (multi-type) transports at a
                    // lower priority. Other units are excluded.
                    case INFANTRY_CARRIER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(INFANTRY_CARRIER)) {
                            avRating += medium_adjust;
                        } else if (mRec.getRoles().contains(TROOP_CARRIER)) {
                            avRating -= min_adjust;
                        } else {
                            return null;
                        }
                        break;

                    // Calling for BA_CARRIER will only return units with this role
                    case BA_CARRIER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(BA_CARRIER)) {
                            return null;
                        }
                        break;

                    // Calling for MECH_CARRIER may include troop (multi-type) transports at a
                    // lower priority. Other units are excluded.
                    case MECH_CARRIER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(MECH_CARRIER)) {
                            avRating += medium_adjust;
                        } else if (mRec.getRoles().contains(TROOP_CARRIER)) {
                            avRating -= min_adjust;
                        } else {
                            return null;
                        }
                        break;

                    // Calling for PROTOMECH_CARRIER will only return units with this role
                    case PROTOMECH_CARRIER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(PROTOMECH_CARRIER)) {
                            return null;
                        }
                        break;

                    // Calling for TUG will only return units with this role
                    case TUG:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(TUG)) {
                            return null;
                        }
                        break;

                    // Calling for POCKET_WARSHIP may include assault DropShips at a
                    // lower priority. Other units are excluded.
                    case POCKET_WARSHIP:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (mRec.getRoles().contains(POCKET_WARSHIP)) {
                            avRating += medium_adjust;
                        } else if (mRec.getRoles().contains(ASSAULT)) {
                            avRating -= min_adjust;
                        } else {
                            return null;
                        }
                        break;

                    // Calling for ASF_CARRIER (fighter carrier) will only return units with this
                    // role
                    case ASF_CARRIER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(ASF_CARRIER)) {
                            return null;
                        }
                        break;

                    // Calling for TROOP_CARRIER (multi-unit transport) will only return units with
                    // this role
                    case TROOP_CARRIER:
                        if (isSpecialized(desiredRoles, mRec)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(TROOP_CARRIER)) {
                            return null;
                        }
                        break;

                    // Calling for a WarShip role excludes all units without that specific role.
                    // Because classes are not explicitly defined, this requires manual role
                    // assignment.

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

                    // Calling for ENGINEER (combat engineers) may include non-engineering units
                    // with the APC, CARGO, SUPPORT, MINELAYER, or MINESWEEPER role at a lower
                    // priority. Units without those roles are excluded.
                    case ENGINEER:
                        if (mRec.getRoles().contains(ENGINEER)) {
                            avRating += medium_adjust;
                            if ((desiredRoles.contains(CARGO) &&
                                    mRec.getRoles().contains(CARGO)) ||
                                    (desiredRoles.contains(MINESWEEPER) &&
                                            mRec.getRoles().contains(MINESWEEPER)) ||
                                    (desiredRoles.contains(MINELAYER) &&
                                            mRec.getRoles().contains(MINELAYER))) {
                                avRating += light_adjust;
                            }
                        } else {
                            if (mRec.getRoles().contains(APC) &&
                                    (mRec.getMovementMode() == EntityMovementMode.WHEELED) ||
                                    mRec.getMovementMode() == EntityMovementMode.TRACKED) {
                                avRating -= light_adjust;
                            } else if ((mRec.getRoles().contains(MINESWEEPER) &&
                                    !desiredRoles.contains(MINESWEEPER)) ||
                                    (mRec.getRoles().contains(MINELAYER)) &&
                                            !desiredRoles.contains(MINELAYER)) {
                                avRating -= min_adjust;
                            } else if (mRec.getRoles().contains(SUPPORT)) {
                                if (mRec.getRoles().contains(CARGO) &&
                                        (mRec.getMovementMode() == EntityMovementMode.WHEELED ||
                                                mRec.getMovementMode() == EntityMovementMode.TRACKED ||
                                                mRec.getMovementMode() == EntityMovementMode.VTOL)) {
                                    avRating -= min_adjust;
                                } else if (!desiredRoles.contains(SUPPORT)) {
                                    avRating -= light_adjust;
                                } else {
                                    return null;
                                }
                            } else {
                                return null;
                            }
                        }
                        break;

                    // Calling for MINESWEEPER only returns units with that role
                    case MINESWEEPER:
                        if (mRec.getRoles().contains(CIVILIAN) &&
                                !desiredRoles.contains(CIVILIAN)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(MINESWEEPER)) {
                            return null;
                        }
                        break;

                    // Calling for MINELAYER only returns units with that role
                    case MINELAYER:
                        if (mRec.getRoles().contains(CIVILIAN) &&
                                !desiredRoles.contains(CIVILIAN)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(MINELAYER)) {
                            return null;
                        }
                        break;

                    // Calling for SUPPORT non-combat units may include units with the APC,
                    // CIVILIAN, CARGO, or ENGINEER roles at a lower priority. This should filter out
                    // all combat units, although some of the selected units may have weapons.
                    case SUPPORT:
                        if (mRec.getRoles().contains(SUPPORT)){
                            avRating += medium_adjust;
                            if (mRec.getRoles().contains(ENGINEER) ||
                                    mRec.getRoles().contains(CARGO)) {
                                avRating += min_adjust;
                            }
                        } else {
                            if (mRec.getRoles().contains(APC) &&
                                    (mRec.getMovementMode() == EntityMovementMode.WHEELED ||
                                            mRec.getMovementMode() == EntityMovementMode.TRACKED ||
                                            mRec.getMovementMode() == EntityMovementMode.VTOL)) {
                                avRating -= light_adjust;
                            } else if (mRec.getRoles().contains(ENGINEER) ||
                                    (mRec.getRoles().contains(CARGO) &&
                                            !mRec.getRoles().contains(CIVILIAN) &&
                                            (mRec.getMovementMode() == EntityMovementMode.WHEELED ||
                                                    mRec.getMovementMode() == EntityMovementMode.TRACKED ||
                                                    mRec.getMovementMode() == EntityMovementMode.VTOL))) {
                                avRating -= min_adjust;
                            } else if (mRec.getRoles().contains(CIVILIAN) &&
                                    mRec.getRoles().contains(CARGO)) {
                                avRating -= light_adjust;
                            } else {
                                return null;
                            }
                        }
                        break;

                    // Calling for CARGO will exclude all units without the role. Units with
                    // additional SUPPORT, CIVILIAN, or ENGINEER roles are handled with those
                    // specific roles.
                    case CARGO:
                        if (mRec.getRoles().contains(CIVILIAN) &&
                                !desiredRoles.contains(CIVILIAN)) {
                            return null;
                        }
                        if (!mRec.getRoles().contains(CARGO)) {
                            return null;
                        }
                        break;

                    // Calling for civilian units will exclude non-civilian units, including
                    // SUPPORT (military non-combat) units.
                    case CIVILIAN:
                        if (!mRec.getRoles().contains(CIVILIAN) ||
                                (mRec.getRoles().contains(CIVILIAN) &&
                                        mRec.getRoles().contains(SUPPORT))) {
                            return null;
                        }
                        break;

                    default:
                        roleApplied = false;
                }

            }
        }

        // If no roles are required, or a role was requested that was not handled, then revert to
        // generic checking.  This is much simpler, only checking for a few exclusions otherwise
        // using the unmodified availability values.
        if (!roleApplied) {

            // DropShips and JumpShips are excluded from non-combat and civilian role
            // checks
            if (mRec.getUnitType() != UnitType.DROPSHIP &&
                    mRec.getUnitType() != UnitType.JUMPSHIP) {

                // Units with the non-combat SUPPORT or CIVILIAN roles should not be used in
                // a general context
                if (mRec.getRoles().contains(SUPPORT) || mRec.getRoles().contains(CIVILIAN)) {
                    return null;
                }

                // Units with only the artillery or missile artillery role should not be used in
                // a general context
                if (mRec.getRoles().size() == 1 &&
                        (mRec.getRoles().contains(ARTILLERY) ||
                                mRec.getRoles().contains(MISSILE_ARTILLERY))) {
                    return null;
                }

            }
        }

        return avRating;
    }

    /**
     * Specialized units are those which are not intended to see direct combat. This includes
     * non-combat military units, civilian units, and units only intended to provide long distance
     * artillery fire.
     * @param desiredRoles Roles which this unit should support
     * @param mRec         Unit to check
     * @return             true, if unit is non-combat and desired roles are combat
     */
    private static boolean isSpecialized(Collection<MissionRole> desiredRoles,
            ModelRecord mRec) {

        // Only units with role tags can be specialized
        if (mRec.getRoles().isEmpty()) {
            return false;
        }

        // Non-combat SUPPORT role being requested in a combat role is considered specialised
        if (mRec.getRoles().contains(SUPPORT) && !desiredRoles.contains(SUPPORT)) {
            return true;
        }

        // Civilian units are always considered specialized unless specifically asked for
        if (mRec.getRoles().contains(CIVILIAN) && !desiredRoles.contains(CIVILIAN)) {
            return true;
        }

        // Units that only provide artillery are considered specialized enough to be non-combat.
        // DropShips are excluded from this check as they provide additional functions even if they
        // mount artillery.
        return (mRec.getUnitType() != UnitType.DROPSHIP) &&
                (mRec.getRoles().size() == 1) &&
                (mRec.getRoles().contains(ARTILLERY) || mRec.getRoles().contains(MISSILE_ARTILLERY));
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
            case "incindiary":
                return INCENDIARY;
            case "ew support":
                return EW_SUPPORT;
            case "artillery":
                return ARTILLERY;
            case "missile artillery":
                return MISSILE_ARTILLERY;
            case "mixed artillery":
                return  MIXED_ARTILLERY;
            case "anti aircraft":
                return ANTI_AIRCRAFT;
            case "anti infantry":
                return ANTI_INFANTRY;
            case "apc":
                return APC;
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
