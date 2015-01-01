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
public class ISERSmallLaser extends LaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -4997798107691083605L;

    /**
     * 
     */
    public ISERSmallLaser() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "ER Small Laser";
        this.setInternalName("ISERSmallLaser");
        this.addLookupName("IS ER Small Laser");
        this.heat = 2;
        this.damage = 3;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 5;
        this.extremeRange = 8;
        this.waterShortRange = 1;
        this.waterMediumRange = 2;
        this.waterLongRange = 3;
        this.waterExtremeRange = 4;
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.flags |= F_NO_FIRES;
        this.bv = 17;
        this.cost = 11250;
        this.shortAV = 3;
        this.maxRange = RANGE_SHORT;
    }
}
