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
    public static final int AEROSPACEFIGHTER = 9;
    public static final int SMALL_CRAFT = 10;
    public static final int DROPSHIP = 11;
    public static final int JUMPSHIP = 12;
    public static final int WARSHIP = 13;
    public static final int SPACE_STATION = 14;
    public static final int AERO = 15; // Non-differentiated Aerospace, like Escape Pods / Life Boats

    private static String[] names = { "Mek", "Tank", "BattleArmor", "Infantry",
            "ProtoMek", "VTOL", "Naval", "Gun Emplacement", "Conventional Fighter",
             "AeroSpaceFighter", "Small Craft", "Dropship",
            "Jumpship", "Warship", "Space Station", "Aero"};

    public static final int SIZE = names.length;

    /** @deprecated use {@code UnitType.getTypeName(e.getUnitType())} instead */
    @Deprecated
    public static String determineUnitType(Entity e) {
        return getTypeName(e.getUnitType());
    }

    /**
     * Reverse lookup for type integer constant from name
     *
     * @param name  Unit type name
     * @return      The unit type constant. If no match can be found, returns -1.
     */
    public static int determineUnitTypeCode(String name) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /** @deprecated use {@link Entity#getUnitType()} instead */
    @Deprecated
    public static int determineUnitTypeCode(Entity e) {
        return e.getUnitType();
    }

    public static String getTypeName(int type) {
        if ((type >= 0) && (type < SIZE)) {
            return names[type];
        }
        throw new IllegalArgumentException("Unknown unit type");
    }

    public static String getTypeDisplayableName(int type) {
        if ((type >= 0) && (type < SIZE)) {
            return Messages.getString("UnitType." + names[type]);
        }
        throw new IllegalArgumentException("Unknown unit type");
    }

    // series of convenience methods to shorten unit type determination

    /**
     * Whether the given entity is a VTOL
     * @param e the entity to examine
     * @return True or false
     */
    public static boolean isVTOL(Entity e) {
        return e.getEntityType() == Entity.ETYPE_VTOL;
    }

    /**
     * Whether the given entity is a Spheroid dropship
     * @param e the entity to examine
     * @return True or false
     */
    public static boolean isSpheroidDropship(Entity e) {
        return e.isAero() && ((IAero) e).isSpheroid();
    }
}
