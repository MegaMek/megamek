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
public class CLERMediumLaser extends LaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -2063816167191977118L;

    /**
     * 
     */
    public CLERMediumLaser() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "ER Medium Laser";
        this.setInternalName("CLERMediumLaser");
        this.addLookupName("Clan ER Medium Laser");
        this.heat = 5;
        this.damage = 7;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.waterShortRange = 3;
        this.waterMediumRange = 7;
        this.waterLongRange = 10;
        this.waterExtremeRange = 14;
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 108;
        this.cost = 80000;
        this.shortAV = 7;
        this.medAV = 7;
        this.maxRange = RANGE_MED;
    }
}
