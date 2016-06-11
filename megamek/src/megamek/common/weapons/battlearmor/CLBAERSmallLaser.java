/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 12, 2004
 *
 */
package megamek.common.weapons.battlearmor;

import megamek.common.TechConstants;
import megamek.common.weapons.LaserWeapon;


/**
 * @author Andrew Hunter
 */
public class CLBAERSmallLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -4120464315724174929L;

    /**
     *
     */
    public CLBAERSmallLaser() {
        super();
        name = "ER Small Laser";
        setInternalName("CLBAERSmallLaser");
        addLookupName("Clan BA ER Small Laser");
        heat = 2;
        damage = 5;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 4;
        waterExtremeRange = 4;
        tonnage = 0.35f;
        criticals = 2;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        bv = 31;
        cost = 11250;
        shortAV = 5;
        maxRange = RANGE_SHORT;
        introDate = 2867;
        techLevel.put(2867, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(2870, TechConstants.T_CLAN_ADVANCED);
        techLevel.put(2880, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X ,RATING_E ,RATING_D ,RATING_C};
        techRating = RATING_F;
        rulesRefs = "258, TM";

  
    }
}
