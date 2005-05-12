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
    public static final int         T_IS_LEVEL_1    = 0;
    public static final int         T_IS_LEVEL_2    = 1;
    public static final int         T_CLAN_LEVEL_2  = 2;

    /*
     * These apply to entities only
     */
    //An entity with an Inner Sphere chassis that mounts some Clan equipment
    public static final int         T_MIXED_BASE_IS_LEVEL_2 = 3;
    //An entity with a Clan chassis that mounts some Inner Sphere equipment
    public static final int         T_MIXED_BASE_CLAN_LEVEL_2 = 4;
    
    // this made public because MekWars accesses it
    public static final String[]    T_NAMES = {"IS level 1", "IS level 2", 
        "Clan level 2", "Mixed (IS) level 2", "Mixed (Clan) level 2"};

    public static final int SIZE = T_NAMES.length;
    
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

}

