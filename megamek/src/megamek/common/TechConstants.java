/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * TechConstants.java
 *
 * Created on June 11, 2002, 4:35 PM
 */

package megamek.common;

/**
 * Contains some constants representing equipment/unit tech levels
 *
 * @author  Ben
 * @version 
 */
public class TechConstants {

    /*
     * These can apply to entities or individual pieces of equipment
     */
    public static final int         T_ALLOWED_ALL       = -2;
    public static final int         T_TECH_UNKNOWN      = -1;
    public static final int         T_IS_LEVEL_1        = 0;
    public static final int         T_IS_LEVEL_2        = 1;
    public static final int         T_CLAN_LEVEL_2      = 2;

    //These two are only for filtering selections in the MechSelectorDialog
    public static final int         T_IS_LEVEL_2_ALL    = 3;
    public static final int         T_LEVEL_2_ALL       = 4;

    public static final int         T_IS_LEVEL_3        = 5;
    public static final int         T_CLAN_LEVEL_3      = 6;

    public static final int         T_ALL               = 7;

    //Number of legal level 2 choices
    public static final int SIZE_LEVEL_2    = 5;

    // this made public because MekWars accesses it
    // It must match the index to the constant's value.
    public static final String[]    T_NAMES = {"IS_level_1",
                                               "IS_level_2",
                                               "Clan_level_2",
                                               "IS_level_1_and_2",
                                               "All_level_2",
                                               "IS_level_3",
                                               "Clan_level_3",
                                                "All"};

    public static final int SIZE            = T_NAMES.length;

    //This translates the integer above into a simple level number.
    // The "all" selections return -1, since they don't apply to
    // individual units.
    public static final String[]    T_SIMPLE_LEVEL = {"1",
                                                      "2",
                                                      "2",
                                                      "-1",
                                                      "-1",
                                                      "3",
                                                      "3",
                                                      "-1"};

    public static String getLevelName(int level) {
        if (level >= 0 && level < SIZE) {
            return T_NAMES[level];
        }
        else
        {
            throw new IllegalArgumentException("Unknown tech level");            
        }
    }

    public static String getLevelDisplayableName(int level) {
        if (level >= 0 && level < SIZE) {
            return Messages.getString("TechLevel."+T_NAMES[level]);
        }
        else
        {
            throw new IllegalArgumentException("Unknown tech level");            
        }
    }

    public static boolean isLegal(int entityTechlevel, int equipmentTechlevel)
    {
        return isLegal(entityTechlevel, equipmentTechlevel, false);
    }

    /**
     * Returns true if the equipment is legal for a unit with the paired tech levels;
     * Returns false if it is not.
     */
    public static boolean isLegal(int entityTechlevel, int equipmentTechlevel, boolean ignoreUnknown) {
        // If it's allowed to all, ALWAYS return true.
        if (equipmentTechlevel == T_ALLOWED_ALL)
            return true;

        // If it's unknown, we're not gonna be able to check it one way or the other, so...
        if (equipmentTechlevel == T_TECH_UNKNOWN)
            if (ignoreUnknown)
                return true;
            else
                return false;

        // If they match, we're all good.
        if (entityTechlevel == equipmentTechlevel)
            return true;

        // If the entity is level 3, it can legally be mixed tech, so we pretty much just smile and nod.
        if ((entityTechlevel == T_IS_LEVEL_3) || (entityTechlevel == T_CLAN_LEVEL_3))
            return true;

        // If none of the catch-alls above are true, we go to specific cases
        if ((equipmentTechlevel == T_IS_LEVEL_1)
            && ((entityTechlevel == T_IS_LEVEL_2)
                || (entityTechlevel == T_IS_LEVEL_2_ALL)
                || (entityTechlevel == T_LEVEL_2_ALL)
                || (entityTechlevel == T_IS_LEVEL_3)
                || (entityTechlevel == T_ALL)))
            return true;
        if ((equipmentTechlevel == T_IS_LEVEL_2)
            && ((entityTechlevel == T_IS_LEVEL_2_ALL)
                || (entityTechlevel == T_LEVEL_2_ALL)
                || (entityTechlevel == T_IS_LEVEL_3)
                || (entityTechlevel == T_ALL)))
            return true;
        if ((equipmentTechlevel == T_CLAN_LEVEL_2)
            && ((entityTechlevel == T_CLAN_LEVEL_2)
                || (entityTechlevel == T_CLAN_LEVEL_3)
                || (entityTechlevel == T_ALL)))
            return true;
        if ((equipmentTechlevel == T_IS_LEVEL_3)
            && ((entityTechlevel == T_IS_LEVEL_3)
                || (entityTechlevel == T_ALL)))
            return true;
        if ((equipmentTechlevel == T_CLAN_LEVEL_3)
            && (entityTechlevel == T_ALL))
            return true;
        return false;
    }
}

