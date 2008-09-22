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
public class CLImprovedHeavyLargeLaser extends LaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 4467522144065588079L;

    /**
     * 
     */
    public CLImprovedHeavyLargeLaser() {
        super();
        this.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        this.name = "Improved Heavy Large Laser";
        this.setInternalName("CLImprovedHeavyLargeLaser");
        this.addLookupName("Clan Improved Large Heavy Laser");
        this.heat = 18;
        this.damage = 16;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 9;
        this.waterExtremeRange = 12;
        this.tonnage = 4.0f;
        this.criticals = 3;
        this.bv = 296;
        this.cost = 350000;
        this.shortAV = 16;
        this.medAV = 16;
        this.maxRange = RANGE_MED;
        this.explosionDamage = 8;
        this.explosive = true;
    }
}
