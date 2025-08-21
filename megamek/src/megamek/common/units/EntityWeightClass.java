/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Messages;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.GunEmplacement;

/**
 * weight class limits and names
 */
public class EntityWeightClass {

    // BA, Mek / Vee / Generic Weights
    public static final int WEIGHT_ULTRA_LIGHT = 0; // Also used for BA "PAL" - Not yet truly implemented for meks
    public static final int WEIGHT_LIGHT = 1; // Conventional Fighters (50t max weight) always return this.
    public static final int WEIGHT_MEDIUM = 2;
    public static final int WEIGHT_HEAVY = 3;
    public static final int WEIGHT_ASSAULT = 4;
    public static final int WEIGHT_COLOSSAL = 5;
    public static final int WEIGHT_SUPER_HEAVY = 5;

    // AeroSpace Units
    public static final int WEIGHT_SMALL_CRAFT = 6; // Only a single weight class for Small Craft
    public static final int WEIGHT_SMALL_DROP = 7;
    public static final int WEIGHT_MEDIUM_DROP = 8;
    public static final int WEIGHT_LARGE_DROP = 9;
    public static final int WEIGHT_SMALL_WAR = 10;
    public static final int WEIGHT_LARGE_WAR = 11;

    // Support Vehicles
    public static final int WEIGHT_SMALL_SUPPORT = 12;
    public static final int WEIGHT_MEDIUM_SUPPORT = 13;
    public static final int WEIGHT_LARGE_SUPPORT = 14;

    // Total number of unique unit weight designations. Should be 1 more than the
    // number above.
    public static final int SIZE = 15;

    private static final String[] classAppends = { "0", "1", "2", "3", "4", "5", "SC", "DS.7", "DS.8", "DS.9", "JS.10",
                                                   "JS.11", "SV.12", "SV.13", "SV.14" };

    private static final double LESS_THAN_5 = Math.nextAfter(5.0, Double.NEGATIVE_INFINITY);
    private static final double[] BAWeightLimits = { 0.4, 0.75, 1, 1.5, 2 };
    private static final double[] mekWeightLimits = { 15, 35, 55, 75, 100, 135 };
    private static final double[] vehicleWeightLimits = { 0, 39, 59, 79, 100, 300 }; // One padding 0
    private static final double[] wheeledSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, LESS_THAN_5,
                                                                        80, 160 }; // Twelve padding 0s
    private static final double[] trackedSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, LESS_THAN_5,
                                                                        100, 200 }; // Twelve padding 0s
    private static final double[] hoverSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, LESS_THAN_5,
                                                                      50, 100 }; // Twelve padding 0s
    private static final double[] vtolSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, LESS_THAN_5,
                                                                     30, 60 }; // Twelve padding 0s
    private static final double[] wigeSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, LESS_THAN_5,
                                                                     80, 160 }; // Twelve padding 0s
    private static final double[] airshipSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, LESS_THAN_5,
                                                                        300, 1000 }; // Twelve padding 0s
    private static final double[] fixedWingSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                                                          LESS_THAN_5, 100, 200 }; // Twelve padding 0s
    private static final double[] navalSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, LESS_THAN_5,
                                                                      300, 100000 }; // Twelve padding 0s
    private static final double[] railSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, LESS_THAN_5,
                                                                     300, 600 }; // Twelve padding 0s
    private static final double[] satelliteSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                                                          LESS_THAN_5, 100, 300 }; // Twelve padding 0s
    private static final double[] ASFWeightLimits = { 0, 45, 70, 100 }; // One padding 0
    private static final double[] dropshipWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 2499, 9999,
                                                           100000 }; // Seven padding 0s
    private static final double[] jumpshipWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 749999,
                                                           2500000 }; // Ten padding 0s
    private static final double[] GEWeightLimits = { 0, 15, 40, 90, 150 }; // One padding 0
    private static final double[] protoWeightLimits = { 0, 3, 5, 7, 9, 10 }; // One padding 0

    public static double[] getWeightLimitByType(String type) {
        if (type.equals(UnitType.getTypeName(UnitType.MEK))) {
            return mekWeightLimits;
        } else if (type.equals(UnitType.getTypeName(UnitType.AEROSPACE_FIGHTER))) {
            return ASFWeightLimits;
        } else if (type.equals(UnitType.getTypeName(UnitType.AERO))) {
            return ASFWeightLimits;
        } else if (type.equals(UnitType.getTypeName(UnitType.BATTLE_ARMOR))) {
            return BAWeightLimits;
        } else if (type.equals(UnitType.getTypeName(UnitType.DROPSHIP))) {
            return dropshipWeightLimits;
        } else if (type.equals(UnitType.getTypeName(UnitType.GUN_EMPLACEMENT))) {
            return GEWeightLimits;
        } else if (type.equals(UnitType.getTypeName(UnitType.JUMPSHIP))
              || type.equals(UnitType.getTypeName(UnitType.WARSHIP))
              || type.equals(UnitType.getTypeName(UnitType.SPACE_STATION))) {
            return jumpshipWeightLimits;
        } else if (type.equals(UnitType.getTypeName(UnitType.PROTOMEK))) {
            return protoWeightLimits;
        } else if (type.equals(UnitType.getTypeName(UnitType.TANK)) || type.equals(UnitType.getTypeName(UnitType.NAVAL))
              || type.equals(UnitType.getTypeName(UnitType.VTOL))) {
            return vehicleWeightLimits;
        } else {
            // Sad... and means we've not implemented yet!
            // Default to Meks. Blech.
            return mekWeightLimits;
        }
    }

    /**
     * Retrieves the weight class based on the type string in the unit file.
     *
     * @param tonnage The entity weight
     * @param type    The type string
     *
     * @return The weight class
     */
    public static int getWeightClass(double tonnage, String type) {
        int i;

        if (type.equals(UnitType.getTypeName(UnitType.BATTLE_ARMOR))) {
            for (i = 0; i < (BAWeightLimits.length - 1); i++) {
                if (tonnage <= BAWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals(UnitType.getTypeName(UnitType.INFANTRY))) {
            return WEIGHT_LIGHT;
        } else if (type.equals(UnitType.getTypeName(UnitType.VTOL))
              || type.equals(UnitType.getTypeName(UnitType.NAVAL))
              || type.equals(UnitType.getTypeName(UnitType.TANK))) {
            for (i = WEIGHT_LIGHT; i < (vehicleWeightLimits.length - 1); i++) { // Started at late to bypass padding &
                // save a loop execution
                if (tonnage <= vehicleWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals(UnitType.getTypeName(UnitType.GUN_EMPLACEMENT))) {
            for (i = WEIGHT_LIGHT; i < (GEWeightLimits.length - 1); i++) { // Started at late to bypass padding & save a
                // loop execution
                if (tonnage <= GEWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals(UnitType.getTypeName(UnitType.MEK))) {
            for (i = 0; i < (mekWeightLimits.length - 1); i++) {
                if (tonnage <= mekWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals(UnitType.getTypeName(UnitType.PROTOMEK))) {
            for (i = WEIGHT_LIGHT; i < (protoWeightLimits.length - 1); i++) { // Started late to bypass padding & save a
                // loop execution
                if (tonnage <= protoWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals(UnitType.getTypeName(UnitType.SPACE_STATION))
              || type.equals(UnitType.getTypeName(UnitType.WARSHIP))
              || type.equals(UnitType.getTypeName(UnitType.JUMPSHIP))) {
            for (i = WEIGHT_SMALL_WAR; i < (jumpshipWeightLimits.length - 1); i++) { // Started late to bypass padding &
                // save a loop execution
                if (tonnage <= jumpshipWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals(UnitType.getTypeName(UnitType.DROPSHIP))) {
            for (i = WEIGHT_SMALL_DROP; i < (dropshipWeightLimits.length - 1); i++) { // Started late to bypass padding
                // & save a loop execution
                if (tonnage <= dropshipWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals(UnitType.getTypeName(UnitType.SMALL_CRAFT))) {
            return WEIGHT_SMALL_CRAFT;
        } else if (type.equals("AeroSpaceFighter") || type.equals("Aero") || type.equals("Conventional Fighter")) {
            for (i = WEIGHT_LIGHT; i < (ASFWeightLimits.length - 1); i++) { // Started late to bypass padding & save a
                // loop execution
                if (tonnage <= ASFWeightLimits[i]) {
                    break;
                }
            }
        } else {
            for (i = 0; i < (mekWeightLimits.length - 1); i++) {
                if (tonnage <= mekWeightLimits[i]) {
                    break;
                }
            }
        }

        return i;
    }

    /**
     * Retrieves the weight class for support vehicles based on the subtype string from the unit file
     *
     * @param tonnage The entity weight
     * @param type    The subtype string
     *
     * @return The weight class
     */
    public static int getSupportWeightClass(double tonnage, String type) {
        double[] weightLimits;
        switch (EntityMovementMode.parseFromString(type)) {
            case WHEELED:
                weightLimits = wheeledSupportVehicleWeightLimits;
                break;
            case TRACKED:
                weightLimits = trackedSupportVehicleWeightLimits;
                break;
            case HOVER:
                weightLimits = hoverSupportVehicleWeightLimits;
                break;
            case VTOL:
                weightLimits = vtolSupportVehicleWeightLimits;
                break;
            case WIGE:
                weightLimits = wigeSupportVehicleWeightLimits;
                break;
            case NAVAL:
            case HYDROFOIL:
            case SUBMARINE:
                weightLimits = navalSupportVehicleWeightLimits;
                break;
            case RAIL:
            case MAGLEV:
                weightLimits = railSupportVehicleWeightLimits;
                break;
            case AERODYNE:
                weightLimits = fixedWingSupportVehicleWeightLimits;
                break;
            case AIRSHIP:
                weightLimits = airshipSupportVehicleWeightLimits;
                break;
            case STATION_KEEPING:
                weightLimits = satelliteSupportVehicleWeightLimits;
                break;
            default:
                return WEIGHT_MEDIUM_SUPPORT;
        }
        for (int i = WEIGHT_SMALL_SUPPORT; i < weightLimits.length; i++) {
            if (tonnage <= weightLimits[i]) {
                return i;
            }
        }

        return WEIGHT_MEDIUM_SUPPORT;
    }

    public static int getWeightClass(double tonnage, Entity en) {
        int i;

        // Order of IF statements is important!! Any subclasses must come before their
        // parent class!
        if (en instanceof Mek) {
            for (i = 0; i < (mekWeightLimits.length - 1); i++) {
                if (tonnage <= mekWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof Jumpship) { // Also handles Warships & Space Stations
            for (i = WEIGHT_SMALL_WAR; i < (jumpshipWeightLimits.length - 1); i++) { // Started late to bypass padding &
                // save a loop execution
                if (tonnage <= jumpshipWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof Dropship) {
            for (i = WEIGHT_SMALL_DROP; i < (dropshipWeightLimits.length - 1); i++) { // Started late to bypass padding
                // & save a loop execution
                if (tonnage <= dropshipWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof FixedWingSupport) {
            for (i = WEIGHT_LIGHT; i < (fixedWingSupportVehicleWeightLimits.length - 1); i++) { // Started late to
                // bypass padding & save
                // a loop execution
                if (tonnage <= fixedWingSupportVehicleWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof SmallCraft) {
            return WEIGHT_SMALL_CRAFT;
        } else if (en instanceof Aero) {
            for (i = WEIGHT_LIGHT; i < (ASFWeightLimits.length - 1); i++) { // Started late to bypass padding & save a
                // loop execution
                if (tonnage <= ASFWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof BattleArmor) {
            for (i = 0; i < (BAWeightLimits.length - 1); i++) {
                if (tonnage <= BAWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof Infantry) { // For now infantry don't have weights, put them all under light?
            return WEIGHT_LIGHT;
        } else if (en instanceof GunEmplacement) {
            for (i = WEIGHT_LIGHT; i < (GEWeightLimits.length - 1); i++) { // Started late to bypass padding & save a
                // loop execution
                if (tonnage <= GEWeightLimits[i]) {
                    break;
                }
            }
        } else if ((en instanceof SupportTank) || (en instanceof SupportVTOL)) {
            switch (en.getMovementMode()) {
                case WHEELED:
                    for (i = WEIGHT_SMALL_SUPPORT; i < (wheeledSupportVehicleWeightLimits.length - 1); i++) {
                        if (tonnage <= wheeledSupportVehicleWeightLimits[i]) {
                            break;
                        }
                    }
                    break;
                case TRACKED:
                    for (i = WEIGHT_SMALL_SUPPORT; i < (trackedSupportVehicleWeightLimits.length - 1); i++) {
                        if (tonnage <= trackedSupportVehicleWeightLimits[i]) {
                            break;
                        }
                    }
                    break;
                case HOVER:
                    for (i = WEIGHT_SMALL_SUPPORT; i < (hoverSupportVehicleWeightLimits.length - 1); i++) {
                        if (tonnage <= hoverSupportVehicleWeightLimits[i]) {
                            break;
                        }
                    }
                    break;
                case VTOL:
                    for (i = WEIGHT_SMALL_SUPPORT; i < (vtolSupportVehicleWeightLimits.length - 1); i++) {
                        if (tonnage <= vtolSupportVehicleWeightLimits[i]) {
                            break;
                        }
                    }
                    break;
                case WIGE:
                    for (i = WEIGHT_SMALL_SUPPORT; i < (wigeSupportVehicleWeightLimits.length - 1); i++) {
                        if (tonnage <= wigeSupportVehicleWeightLimits[i]) {
                            break;
                        }
                    }
                    break;
                case NAVAL, SUBMARINE, HYDROFOIL:
                    for (i = WEIGHT_SMALL_SUPPORT; i < (navalSupportVehicleWeightLimits.length - 1); i++) {
                        if (tonnage <= navalSupportVehicleWeightLimits[i]) {
                            break;
                        }
                    }
                    break;
                case RAIL:
                case MAGLEV:
                    for (i = WEIGHT_SMALL_SUPPORT; i < (railSupportVehicleWeightLimits.length - 1); i++) {
                        if (tonnage <= railSupportVehicleWeightLimits[i]) {
                            break;
                        }
                    }
                    break;
                default:
                    i = 0;
                    break;
            }
        } else if (en instanceof Tank) {
            for (i = WEIGHT_LIGHT; i < (vehicleWeightLimits.length - 1); i++) { // Started late to bypass padding & save
                // a loop execution
                if (tonnage <= vehicleWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof ProtoMek) {
            for (i = 0; i < (protoWeightLimits.length - 1); i++) {
                if (tonnage <= protoWeightLimits[i]) {
                    break;
                }
            }
        } else { // And... we'll default to the mek chart.
            for (i = 0; i < (mekWeightLimits.length - 1); i++) {
                if (tonnage <= mekWeightLimits[i]) {
                    break;
                }
            }
        }
        return i;
    }

    public static double getClassLimit(int wClass, Entity en) {
        // Order of IF statements is important!! Any subclasses must come before their
        // parent class!
        if (en instanceof Mek) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return mekWeightLimits[wClass];
            }
        } else if (en instanceof Jumpship) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return jumpshipWeightLimits[wClass];
            }
        } else if (en instanceof Dropship) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return dropshipWeightLimits[wClass];
            }
        } else if (en instanceof ConvFighter) {
            return 50;
        } else if (en instanceof SmallCraft) {
            return 200;
        } else if (en instanceof Aero) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return ASFWeightLimits[wClass];
            }
        } else if (en instanceof BattleArmor) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return BAWeightLimits[wClass];
            }
        } else if (en instanceof Infantry) {
            return 500; // Not a clue for infantry, since I can't find anything for them.
        } else if (en instanceof GunEmplacement) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return GEWeightLimits[wClass];
            }
        } else if (en instanceof Tank) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return vehicleWeightLimits[wClass];
            }
        } else if (en instanceof ProtoMek) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return protoWeightLimits[wClass];
            }
        } else { // And... we'll default to meks
            if ((wClass >= 0) && (wClass < SIZE)) {
                return mekWeightLimits[wClass];
            }
        }
        throw new IllegalArgumentException("Unknown Weight Class");
    }

    /**
     * Get the weight class name given a weight class define and a unitType. The unitType is a string that matches the
     * unit type returned from a MekSummary.
     *
     */
    public static String getClassName(int wClass, String unitType, boolean isSupport) {
        if (unitType.equals(UnitType.getTypeName(UnitType.SPACE_STATION))) {
            return Messages.getString("EntityWeightClass.SS." + wClass);
        }
        if (unitType.equals(UnitType.getTypeName(UnitType.WARSHIP))) {
            return Messages.getString("EntityWeightClass.WS." + wClass);
        }
        if (unitType.equals(UnitType.getTypeName(UnitType.JUMPSHIP))) {
            return Messages.getString("EntityWeightClass.JS." + wClass);
        }
        if (unitType.equals(UnitType.getTypeName(UnitType.DROPSHIP))) {
            return Messages.getString("EntityWeightClass.DS." + wClass);
        }
        if (unitType.equals(UnitType.getTypeName(UnitType.SMALL_CRAFT))) {
            return Messages.getString("EntityWeightClass.SC");
        }
        if (isSupport) {
            return Messages.getString("EntityWeightClass.SV." + wClass);
        }
        if ((wClass >= 0) && (wClass < SIZE)) {
            return Messages.getString("EntityWeightClass." + wClass);
        }
        throw new IllegalArgumentException("Unknown Weight Class in getClassName(int, string, boolean)");
    }

    public static String getClassName(int wClass, Entity en) {
        return getClassName(wClass, UnitType.getTypeName(en.getUnitType()), en.isSupportVehicle());
    }

    public static String getClassName(int nameVal) {
        if ((nameVal >= 0) && (nameVal < SIZE)) {
            return Messages.getString("EntityWeightClass." + classAppends[nameVal]);
        }
        throw new IllegalArgumentException("Unknown Weight Class in getClassName(int)");
    }

}
