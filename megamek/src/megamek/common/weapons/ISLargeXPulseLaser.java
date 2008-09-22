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
 * Created on Sep 8, 20045
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISLargeXPulseLaser extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -8159582350685114767L;

    /**
     * 
     */
    public ISLargeXPulseLaser() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "Large X-Pulse Laser";
        this.setInternalName("ISLargeXPulseLaser");
        this.addLookupName("IS X-Pulse Large Laser");
        this.addLookupName("IS Large X-Pulse Laser");
        this.heat = 14;
        this.damage = 9;
        this.toHitModifier = -2;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.waterShortRange = 2;
        this.waterMediumRange = 5;
        this.waterLongRange = 7;
        this.waterExtremeRange = 10;
        this.tonnage = 7.0f;
        this.criticals = 2;
        this.bv = 178;
        this.cost = 275000;
    }
}
