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
package megamek.common.weapons;

import megamek.common.EquipmentType;
import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class CLERSmallLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -4120464315724174929L;

    /**
     *
     */
    public CLERSmallLaser() {
        super();
        techLevel.put(3071,TechConstants.T_CLAN_TW);
        name = "ER Small Laser";
        setInternalName("CLERSmallLaser");
        addLookupName("Clan ER Small Laser");
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
        tonnage = 0.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 31;
        cost = 11250;
        shortAV = 5;
        maxRange = RANGE_SHORT;
        introDate = 2825;
        techLevel.put(2825,techLevel.get(3071));
        availRating = new int[]{RATING_X,RATING_X,RATING_D};
        techRating = RATING_F;
    }
}
