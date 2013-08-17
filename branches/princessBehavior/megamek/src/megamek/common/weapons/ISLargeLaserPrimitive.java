/**
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Deric Page deric.page@usa.net
 */
public class ISLargeLaserPrimitive extends LaserWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 6640106383069896198L;

    public ISLargeLaserPrimitive() {
        super();
        this.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        this.name = "Large Laser Prototype";
        this.setInternalName(this.name);
        this.addLookupName("IS Large Laser Prototype");
        this.addLookupName("ISLargeLaserPrototype");
        this.heat = 12;
        this.damage = 8;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 9;
        this.waterExtremeRange = 12;
        this.tonnage = 5.0f;
        this.criticals = 2;
        this.bv = 123;
        this.cost = 100000;
        this.shortAV = 8;
        this.medAV = 8;
        this.maxRange = RANGE_MED;
    }
}
