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
import megamek.common.WeaponType;

/**
 * @author Jason Tighe
 */
public class ISLargeVariableSpeedPulseLaser extends VariableSpeedPulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 2676144961105838316L;

    /**
     * 
     */
    public ISLargeVariableSpeedPulseLaser() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Large VSP";
        this.setInternalName("ISLargeVSPLaser");
        this.setInternalName("ISLVSPL");
        this.setInternalName("ISLargeVariableSpeedLaser");
        this.heat = 10;
        this.damage = WeaponType.DAMAGE_VARIABLE;
        this.toHitModifier = -4;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 15;
        this.extremeRange = 16;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 10;
        this.waterExtremeRange = 12;
        this.damageShort = 11;
        this.damageMedium = 9;
        this.damageLong = 7;
        this.tonnage = 9.0f;
        this.criticals = 4;
        this.bv = 123;
        this.cost = 465000;
        this.shortAV = 7;
        this.medAV = 10;
        this.maxRange = RANGE_MED;
    }
    
}
