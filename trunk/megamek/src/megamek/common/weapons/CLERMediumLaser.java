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
public class CLERMediumLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -2063816167191977118L;

    /**
     *
     */
    public CLERMediumLaser() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_TW);
        name = "ER Medium Laser";
        setInternalName("CLERMediumLaser");
        addLookupName("Clan ER Medium Laser");
        heat = 5;
        damage = 7;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 7;
        waterLongRange = 10;
        waterExtremeRange = 14;
        tonnage = 1.0f;
        criticals = 1;
        bv = 108;
        cost = 80000;
        shortAV = 7;
        medAV = 7;
        maxRange = RANGE_MED;
        availRating = new int[] { EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D };
        introDate = 2824;
        techLevel.put(2824, techLevel.get(3071));
        techRating = RATING_F;
    }
}
