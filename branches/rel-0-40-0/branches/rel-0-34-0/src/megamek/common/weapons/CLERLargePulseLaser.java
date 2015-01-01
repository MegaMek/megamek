/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
 * @author Sebastian Brocks
 */
public class CLERLargePulseLaser extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -5795252987498124086L;

    /**
     * 
     */
    public CLERLargePulseLaser() {
        super();
        this.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        this.name = "ER Large Pulse Laser";
        this.setInternalName("CLERLargePulseLaser");
        this.addLookupName("Clan ER Pulse Large Laser");
        this.addLookupName("Clan ER Large Pulse Laser");
        this.heat = 13;
        this.damage = 10;
        this.toHitModifier = -1;
        this.shortRange = 7;
        this.mediumRange = 15;
        this.longRange = 23;
        this.extremeRange = 30;
        this.waterShortRange = 4;
        this.waterMediumRange = 10;
        this.waterLongRange = 14;
        this.waterExtremeRange = 20;
        this.tonnage = 6.0f;
        this.criticals = 3;
        this.bv = 271;
        this.cost = 400000;
    }
}
