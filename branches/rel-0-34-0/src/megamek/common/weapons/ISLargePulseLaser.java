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
public class ISLargePulseLaser extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 94533476706680275L;

    /**
     * 
     */
    public ISLargePulseLaser() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Large Pulse Laser";
        this.setInternalName("ISLargePulseLaser");
        this.addLookupName("IS Pulse Large Laser");
        this.addLookupName("IS Large Pulse Laser");
        this.heat = 10;
        this.damage = 9;
        this.toHitModifier = -2;
        this.shortRange = 3;
        this.mediumRange = 7;
        this.longRange = 10;
        this.extremeRange = 14;
        this.waterShortRange = 2;
        this.waterMediumRange = 5;
        this.waterLongRange = 7;
        this.waterExtremeRange = 10;
        this.tonnage = 7.0f;
        this.criticals = 2;
        this.bv = 119;
        this.cost = 175000;
        this.flags |= F_PULSE;
        this.shortAV = 9;
        this.medAV = 9;
        this.maxRange = RANGE_MED;
    }
}
