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

import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import megamek.common.weapons.ISSmallLaser;

/**
 * @author Jay Lawson
 */
public class ISBASmallLaser extends ISSmallLaser {
    
    /**
     * 
     */
    private static final long serialVersionUID = -4033152775138299857L;

    public ISBASmallLaser() {
        super();
        techLevel.put(3071, TechConstants.T_INTRO_BOXSET);
        name = "BA Small Laser";
        setInternalName(name);
        addLookupName("ISBASmall Laser");
        addLookupName("ISBASmallLaser");
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
        tonnage = 0.2f;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON);
        bv = 9;
        cost = 11250;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        this.availRating = new int[] { EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_B };
        introDate = 2400;
        techLevel.put(2400, techLevel.get(3071));
        techRating = RATING_E;
        name = "BA Small Laser";
    }
    
}