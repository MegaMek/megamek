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
 * @author Ben
 * @version
 */
public class TechConstants {

    /*
     * These can apply to entities or individual pieces of equipment
     */
    public static final int T_ALLOWED_ALL = -2;
    public static final int T_TECH_UNKNOWN = -1;
    public static final int T_INTRO_BOXSET = 0;
    public static final int T_IS_TW_NON_BOX = 1;
    public static final int T_CLAN_TW = 2;

    // These two are only for filtering selections in the MechSelectorDialog
    public static final int T_IS_TW_ALL = 3;
    public static final int T_TW_ALL = 4;

    public static final int T_IS_ADVANCED = 5;
    public static final int T_CLAN_ADVANCED = 6;
    
    public static final int T_IS_EXPERIMENTAL = 7;
    public static final int T_CLAN_EXPERIMENTAL = 8;
    
    public static final int T_IS_UNOFFICIAL = 9;
    public static final int T_CLAN_UNOFFICIAL = 10;

    public static final int T_ALL = 11;

    // Number of legal level 2 choices
    public static final int SIZE_LEVEL_2 = 5;

    // this made public because MekWars accesses it
    // It must match the index to the constant's value.
    public static final String[] T_NAMES = { "IS_Box_Set", "IS_TW_Non_Box",
            "Clan_TW", "IS_TW", "All_TW", "IS_Advanced",
            "Clan_Advanced", "IS_Experimental", "Clan_Experimental", "IS_Unofficial", "Clan_Unofficial", "All" };

    public static final int SIZE = T_NAMES.length;

    // This translates the integer above into a simple level number.
    // The "all" selections return -1, since they don't apply to
    // individual units.
    public static final String[] T_SIMPLE_LEVEL = { "1", "2", "2", "-1", "-1",
            "3", "3", "4", "4", "5", "5", "-1" };

    public static String getLevelName(int level) {
        if(level == T_ALLOWED_ALL)
            return "(allowed to all)";
        if(level == T_TECH_UNKNOWN)
            return "(unknown tech level)";
        if (level >= 0 && level < SIZE) {
            return T_NAMES[level];
        }
        throw new IllegalArgumentException("Unknown tech level");
    }

    public static String getLevelDisplayableName(int level) {
        if (level >= 0 && level < SIZE) {
            return Messages.getString("TechLevel." + T_NAMES[level]);
        }
        throw new IllegalArgumentException("Unknown tech level");
    }

    public static boolean isLegal(int entityTechlevel, int equipmentTechlevel) {
        return isLegal(entityTechlevel, equipmentTechlevel, false);
    }

    /**
     * Returns true if the equipment is legal for a unit with the paired tech
     * levels; Returns false if it is not.
     */
    public static boolean isLegal(int entityTechlevel, int equipmentTechlevel,
            boolean ignoreUnknown) {
        // If it's allowed to all, ALWAYS return true.
        if (equipmentTechlevel == T_ALLOWED_ALL)
            return true;

        // If it's unknown, we're not gonna be able to check it one way or the
        // other, so...
        if (equipmentTechlevel == T_TECH_UNKNOWN) {
            if (ignoreUnknown)
                return true;
            return false;
        }

        // If they match, we're all good.
        if (entityTechlevel == equipmentTechlevel)
            return true;

        // If the entity is experimental or unofficial, it can legally be mixed
        // tech, so we pretty much just smile and nod.
        if ((entityTechlevel == T_IS_EXPERIMENTAL)
                || (entityTechlevel == T_CLAN_EXPERIMENTAL)
                || (entityTechlevel == T_IS_UNOFFICIAL)
                || (entityTechlevel == T_CLAN_UNOFFICIAL))
            return true;

        // If none of the catch-alls above are true, we go to specific cases
        
        // IS box set can be in any IS, or in clan advanced (mixed tech)
        if ((equipmentTechlevel == T_INTRO_BOXSET)
                && ((entityTechlevel == T_IS_TW_NON_BOX)
                        || (entityTechlevel == T_IS_TW_ALL)
                        || (entityTechlevel == T_TW_ALL)
                        || (entityTechlevel == T_IS_ADVANCED)
                        || (entityTechlevel == T_CLAN_ADVANCED)
                        || (entityTechlevel == T_ALL)))
            return true;
        
        // IS TW stuff can be in any IS unit, or in clan advanced (mixed tech)
        if ((equipmentTechlevel == T_IS_TW_NON_BOX)
                && ((entityTechlevel == T_IS_TW_ALL)
                        || (entityTechlevel == T_TW_ALL)
                        || (entityTechlevel == T_IS_ADVANCED) 
                        || (entityTechlevel == T_CLAN_ADVANCED)
                        || (entityTechlevel == T_ALL)))
            return true;
        // clan TW stuff can be in any clan, or in IS advanced (mixed tech)
        if ((equipmentTechlevel == T_CLAN_TW)
                && ((entityTechlevel == T_CLAN_TW)
                        || (entityTechlevel == T_CLAN_ADVANCED)
                        || (entityTechlevel == T_IS_ADVANCED)
                        || (entityTechlevel == T_ALL)))
            return true;
        // IS advanced stuff can be in clan advanced (IS advanced is caught by 
        // the identical level if further up)
        if ((equipmentTechlevel == T_IS_ADVANCED)
                && ((entityTechlevel == T_CLAN_ADVANCED)
                        || (entityTechlevel == T_ALL)))
            return true;
        // clan advanced stuff can be in IS advanced (clan advanced is caught by 
        // the identical level if further up)
        if ((equipmentTechlevel == T_CLAN_ADVANCED)
                && ((entityTechlevel == T_IS_ADVANCED))
                || (entityTechlevel == T_ALL))
            return true;
        // no need to check for the unofficial stuff, that is caught by identical
        // level further up or by the unofficial check
        return false;
    }

    public static String getTechName(int level) {
        if (level == T_INTRO_BOXSET || level == T_IS_TW_NON_BOX
                || level == T_IS_ADVANCED || level == T_IS_EXPERIMENTAL
                || level == T_IS_UNOFFICIAL)
            return "Inner Sphere";
        else if (level == T_CLAN_TW || level == T_CLAN_ADVANCED
                || level == T_CLAN_EXPERIMENTAL || level == T_CLAN_UNOFFICIAL)
            return "Clan";
        else
            return "(Unknown Technology Base)";
    }
}
