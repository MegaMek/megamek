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
    public static final int AEROSPACE_FIGHTER = 9;
    public static final int SMALL_CRAFT = 10;
    public static final int DROPSHIP = 11;
    public static final int JUMPSHIP = 12;
    public static final int WARSHIP = 13;
    public static final int SPACE_STATION = 14;
    public static final int HANDHELD_WEAPON = 15;
    public static final int MOBILE_STRUCTURE = 16;
    public static final int ADVANCED_BUILDING = 17;
    // IMPORTANT: AERO must be the last unit type for advanced search to work,
    // unless you are adding a new unit type which like Aero should be excluded from search.
    // In that case add an exception for it in the definition of unitTypeModel in AbstractUnitSelectorDialog.java
    public static final int AERO = 18; // Non-differentiated Aerospace, like Escape Pods / Lifeboats

    private static final String[] names = { "Mek", "Tank", "BattleArmor", "Infantry", "ProtoMek", "VTOL", "Naval",
                                            "Gun Emplacement", "Conventional Fighter", "AeroSpaceFighter",
                                            "Small Craft", "Dropship", "Jumpship", "Warship", "Space Station",
                                            "Handheld Weapon", "Mobile Structure", "Advanced Building", "Aero" };

    public static final int SIZE = names.length;

    /**
     * Reverse lookup for type integer constant from name
     *
     * @param name Unit type name
     *
     * @return The unit type constant. If no match can be found, returns -1.
     */
    public static int determineUnitTypeCode(String name) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return i;
            }
        }
        return -1;
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
     *
     * @param e the entity to examine
     *
     * @return True or false
     */
    public static boolean isVTOL(Entity e) {
        return e.getEntityType() == Entity.ETYPE_VTOL;
    }

    /**
     * Whether the given entity is a Spheroid dropship
     *
     * @param e the entity to examine
     *
     * @return True or false
     */
    public static boolean isSpheroidDropship(Entity e) {
        return e.isAero() && ((IAero) e).isSpheroid();
    }
}
