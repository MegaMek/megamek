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

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISERMediumLaser extends LaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 309223577642811605L;

    /**
     * 
     */
    public ISERMediumLaser() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "ER Medium Laser";
        this.setInternalName("ISERMediumLaser");
        this.addLookupName("IS ER Medium Laser");
        this.heat = 5;
        this.damage = 5;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.waterShortRange = 3;
        this.waterMediumRange = 5;
        this.waterLongRange = 8;
        this.waterExtremeRange = 10;
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 62;
        this.cost = 80000;
        this.shortAV = 5;
        this.medAV = 5;
        this.maxRange = RANGE_MED;
    }
}
