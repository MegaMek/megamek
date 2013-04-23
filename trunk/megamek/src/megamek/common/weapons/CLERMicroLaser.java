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
public class CLERMicroLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -445880139385652098L;

    /**
     *
     */
    public CLERMicroLaser() {
        super();
        techLevel = TechConstants.T_CLAN_TW;
        name = "ER Micro Laser";
        setInternalName("CLERMicroLaser");
        addLookupName("Clan ER Micro Laser");
        heat = 1;
        damage = 2;
        shortRange = 1;
        mediumRange = 2;
        longRange = 4;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 4;
        tonnage = 0.25f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 7;
        cost = 10000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        this.availRating = new int[]{EquipmentType.RATING_X, EquipmentType.RATING_X,EquipmentType.RATING_D};
        this.introDate = 3060;
        techRating = RATING_F;
        }
}
