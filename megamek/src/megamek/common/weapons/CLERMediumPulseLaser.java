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
public class CLERMediumPulseLaser extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 7816191920104768204L;

    /**
     * 
     */
    public CLERMediumPulseLaser() {
        super();
        this.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        this.name = "ER Medium Pulse Laser";
        this.setInternalName("CLERMediumPulseLaser");
        this.addLookupName("Clan ER Pulse Med Laser");
        this.addLookupName("Clan ER Medium Pulse Laser");
        this.heat = 6;
        this.damage = 7;
        this.toHitModifier = -1;
        this.shortRange = 5;
        this.mediumRange = 9;
        this.longRange = 14;
        this.extremeRange = 18;
        this.waterShortRange = 3;
        this.waterMediumRange = 5;
        this.waterLongRange = 8;
        this.waterExtremeRange = 10;
        this.tonnage = 2.0f;
        this.criticals = 2;
        this.bv = 116;
        this.cost = 150000;
    }
}
