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
public interface TechConstants {

    public static final int         T_IS_LEVEL_1    = 0;
    public static final int         T_IS_LEVEL_2    = 1;
    public static final int         T_CLAN_LEVEL_2  = 2;
    public static final int         T_MIXED_LEVEL_2 = 3;
    
    public static final String[]    T_NAMES = {"IS level 1", "IS level 2", 
        "Clan level 2", "Mixed level 2"};
}

