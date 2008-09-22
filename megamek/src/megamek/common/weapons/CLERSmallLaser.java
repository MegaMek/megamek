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
public class CLERSmallLaser extends LaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -4120464315724174929L;

    /**
     * 
     */
    public CLERSmallLaser() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "ER Small Laser";
        this.setInternalName("CLERSmallLaser");
        this.addLookupName("Clan ER Small Laser");
        this.heat = 2;
        this.damage = 5;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.waterShortRange = 1;
        this.waterMediumRange = 2;
        this.waterLongRange = 4;
        this.waterExtremeRange = 4;
        this.tonnage = 0.5f;
        this.criticals = 1;
        this.flags |= F_NO_FIRES;
        this.bv = 31;
        this.cost = 11250;
        this.shortAV = 5;
        this.maxRange = RANGE_SHORT;
    }
}
