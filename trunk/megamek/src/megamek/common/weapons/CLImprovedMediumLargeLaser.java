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
public class CLImprovedMediumLargeLaser extends LaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 4467522144065588079L;

    /**
     * 
     */
    public CLImprovedMediumLargeLaser() {
        super();
        this.techLevel = TechConstants.T_CLAN_LEVEL_2;
        this.name = "Improved Medium Large Laser";
        this.setInternalName("CLImprovedMediumLargeLaser");
        this.addLookupName("Clan Improved Large Medium Laser");
        this.heat = 7;
        this.damage = 10;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.waterShortRange = 2;
        this.waterMediumRange = 5;
        this.waterLongRange = 8;
        this.waterExtremeRange = 10;
        this.tonnage = 1.0f;
        this.criticals = 2;
        this.bv = 93;
        this.cost = 150000;
        this.shortAV = 10;
        this.maxRange = RANGE_SHORT;
        this.explosionDamage = 5;
        this.explosive = true;
    }
}
