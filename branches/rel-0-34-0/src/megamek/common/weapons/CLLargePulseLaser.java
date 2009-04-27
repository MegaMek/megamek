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
public class CLLargePulseLaser extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 608317914802476438L;

    /**
     * 
     */
    public CLLargePulseLaser() {
        super();
        this.techLevel = TechConstants.T_CLAN_TW;
        this.name = "Large Pulse Laser";
        this.setInternalName("CLLargePulseLaser");
        this.addLookupName("Clan Pulse Large Laser");
        this.addLookupName("Clan Large Pulse Laser");
        this.heat = 10;
        this.damage = 10;
        this.toHitModifier = -2;
        this.shortRange = 6;
        this.mediumRange = 14;
        this.longRange = 20;
        this.extremeRange = 28;
        this.waterShortRange = 4;
        this.waterMediumRange = 10;
        this.waterLongRange = 14;
        this.waterExtremeRange = 20;
        this.tonnage = 6.0f;
        this.criticals = 2;
        this.bv = 265;
        this.cost = 175000;
        this.shortAV = 10;
        this.medAV = 10;
        this.longAV = 10;
        this.maxRange = RANGE_LONG;
    }
}
