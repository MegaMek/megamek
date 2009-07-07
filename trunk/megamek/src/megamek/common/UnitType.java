/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

/**
 * weight class limits and names
 */
public class UnitType {

    public static final int MEK = 0;
    public static final int TANK = 1;
    public static final int BATTLE_ARMOR = 2;
    public static final int INFANTRY = 3;
    public static final int PROTOMEK = 4;
    public static final int VTOL = 5;
    public static final int NAVAL = 6;
    public static final int GUN_EMPLACEMENT = 7;
    public static final int CONV_FIGHTER = 8;
    public static final int AERO = 9;
    public static final int SMALL_CRAFT = 10;
    public static final int DROPSHIP = 11;
    public static final int WARSHIP = 12;
    public static final int JUMPSHIP = 13;
    public static final int SPACE_STATION = 14;

    private static String[] names = { "Mek", "Tank", "BattleArmor", "Infantry",
            "ProtoMek", "VTOL", "Naval", "Gun Emplacement", "Conventional Fighter", 
            "Aero", "Small Craft", "Dropship", 
            "Jumpship", "Warship", "Space Station" };

    public static final int SIZE = names.length;

    public static String determineUnitType(Entity e) {
        return names[determineUnitTypeCode(e)];
    }

    public static int determineUnitTypeCode(Entity e) {
        int mm = e.getMovementMode();
        if (e instanceof Infantry) {
            return INFANTRY;
        } else if (e instanceof VTOL) {
            return VTOL;
        } else if ((mm == IEntityMovementMode.NAVAL)
                || (mm == IEntityMovementMode.HYDROFOIL)
                || (mm == IEntityMovementMode.SUBMARINE)) {
            return NAVAL;
        } else if (e instanceof Tank) {
            return TANK;
        } else if (e instanceof Mech) {
            return MEK;
        } else if (e instanceof Protomech) {
            return PROTOMEK;
        } else if (e instanceof GunEmplacement) {
            return GUN_EMPLACEMENT;
        } else if (e instanceof Warship) {
            return WARSHIP;
        } else if (e instanceof Jumpship) {
            return JUMPSHIP;
        } else if (e instanceof Dropship) {
            return DROPSHIP;
        } else if (e instanceof SmallCraft) {
            return SMALL_CRAFT;
        } else if (e instanceof ConvFighter) {
            return CONV_FIGHTER;
        } else if (e instanceof Aero) {
            return AERO;
        }else {
            throw new IllegalArgumentException("Unknown unit type");
        }
    }

    public static String getTypeName(int type) {
        if (type >= 0 && type < SIZE) {
            return names[type];
        }
        throw new IllegalArgumentException("Unknown unit type");
    }

    public static String getTypeDisplayableName(int type) {
        if (type >= 0 && type < SIZE) {
            return Messages.getString("UnitType." + names[type]);
        }
        throw new IllegalArgumentException("Unknown unit type");
    }

}
