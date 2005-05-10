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
    
    private static String[] names = {"Mek", "Tank", "Infantry", "ProtoMek"};

    public static final int SIZE = names.length;

    public static String determineUnitType(Entity e) {
        if (e instanceof Infantry) {
            return names[INFANTRY];
        } else if (e instanceof Tank) {
            return names[TANK];
        } else if (e instanceof Mech) {
            return names[MEK];
        } else if (e instanceof Protomech) {
            return names[PROTOMEK];
        } else {
            throw new IllegalArgumentException("Unknown unit type");
        }
    }

    public static String getTypeName(int type) {
        if (type >= 0 && type < SIZE) {
            return names[type];
        } else {
            throw new IllegalArgumentException("Unknown unit type");            
        }
    }

    public static String getTypeDisplayableName(int type) {
        if (type >= 0 && type < SIZE) {
            return Messages.getString("UnitType."+names[type]);
        } else {
            throw new IllegalArgumentException("Unknown unit type");            
        }
    }
    
}
