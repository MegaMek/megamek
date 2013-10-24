/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 2, 2004
 *
 */
package megamek.common.weapons.battlearmor;

import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import megamek.common.weapons.LaserWeapon;

/**
 * @author Jay Lawson
 */
public class ISBAMediumLaser extends LaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 2178224725694704541L;

    public ISBAMediumLaser() {
        super();
        this.techLevel.put(3071, TechConstants.T_INTRO_BOXSET);
        this.name = "BA Medium Laser";
        this.setInternalName(this.name);
        this.addLookupName("IS BA Medium Laser");
        this.addLookupName("ISBAMediumLaser");
        this.damage = 5;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.waterShortRange = 2;
        this.waterMediumRange = 4;
        this.waterLongRange = 6;
        this.waterExtremeRange = 8;
        this.tonnage = 0.5f;
        this.criticals = 3;
        this.bv = 46;
        this.cost = 40000;
        this.shortAV = 5;
        flags = flags.or(F_BA_WEAPON);
        this.maxRange = RANGE_SHORT;
        this.availRating = new int[] { EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_B };
        introDate = 2400;
        techLevel.put(2400, techLevel.get(3071));
        techRating = RATING_E;
    }
}
