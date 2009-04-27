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
public class CLERLargeLaser extends LaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -3799823521215134292L;

    /**
     * 
     */
    public CLERLargeLaser() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "ER Large Laser";
        this.setInternalName("CLERLargeLaser");
        this.addLookupName("Clan ER Large Laser");
        this.heat = 12;
        this.damage = 10;
        this.shortRange = 8;
        this.mediumRange = 15;
        this.longRange = 25;
        this.extremeRange = 30;
        this.waterShortRange = 5;
        this.waterMediumRange = 10;
        this.waterLongRange = 16;
        this.waterExtremeRange = 20;
        this.tonnage = 4.0f;
        this.criticals = 1;
        this.bv = 248;
        this.cost = 200000;
        this.shortAV = 10;
        this.medAV = 10;
        this.longAV = 10;
        this.extAV = 10;
        this.maxRange = RANGE_EXT;
    }
}
