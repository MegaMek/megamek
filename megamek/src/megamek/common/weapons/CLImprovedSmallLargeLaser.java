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
 * @author Jason Tighe
 */
public class CLImprovedSmallLargeLaser extends LaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 4467522144065588079L;

    /**
     * 
     */
    public CLImprovedSmallLargeLaser() {
        super();
        this.techLevel = TechConstants.T_CLAN_LEVEL_2;
        this.name = "Improved Small Large Laser";
        this.setInternalName("CLImprovedSmallLargeLaser");
        this.addLookupName("Clan Improved Small Heavy Laser");
        this.heat = 3;
        this.damage = 6;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.waterShortRange = 2;
        this.waterMediumRange = 3;
        this.waterLongRange = 5;
        this.waterExtremeRange = 6;
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.bv = 19;
        this.cost = 30000;
        this.shortAV = 6;
        this.maxRange = RANGE_SHORT;
        this.explosionDamage = 3;
        this.explosive = true;
    }
}
