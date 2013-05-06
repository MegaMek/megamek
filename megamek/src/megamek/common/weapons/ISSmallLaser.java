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
public class ISSmallLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 7750443222466213123L;

    public ISSmallLaser() {
        super();
        techLevel.put(3071,TechConstants.T_INTRO_BOXSET);
        name = "Small Laser";
        setInternalName(name);
        addLookupName("ISSmall Laser");
        addLookupName("ISSmallLaser");
        heat = 1;
        damage = 3;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 4;
        tonnage = 0.5f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 9;
        cost = 11250;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        this.availRating = new int[]{EquipmentType.RATING_B, EquipmentType.RATING_B,EquipmentType.RATING_B};
        this.introDate = 2400;
        techRating = RATING_C;
    }
}
