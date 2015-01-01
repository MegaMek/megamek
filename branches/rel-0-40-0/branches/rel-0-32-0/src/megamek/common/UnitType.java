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
    public static final int INFANTRY = 2;
    public static final int PROTOMEK = 3;
    public static final int VTOL = 4;
    public static final int NAVAL = 5;
    public static final int GUN_EMPLACEMENT = 6;
    
    private static String[] names = {"Mek", "Tank", "Infantry", "ProtoMek", "VTOL", "Naval", "Gun Emplacement"};

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
        } else if ((mm == IEntityMovementMode.NAVAL) || (mm == IEntityMovementMode.HYDROFOIL) || (mm == IEntityMovementMode.SUBMARINE)) {
            return NAVAL;
        } else if (e instanceof Tank) {
            return TANK;
        } else if (e instanceof Mech) {
            return MEK;
        } else if (e instanceof Protomech) {
            return PROTOMEK;
        } else if (e instanceof GunEmplacement) {
            return GUN_EMPLACEMENT;
        } else {
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
            return Messages.getString("UnitType."+names[type]);
        }
        throw new IllegalArgumentException("Unknown unit type");
    }
    
}
