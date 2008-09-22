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
public class ISSmallVariableSpeedPulseLaser extends VariableSpeedPulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 2676144961105838316L;

    /**
     * 
     */
    public ISSmallVariableSpeedPulseLaser() {
        super();
        this.techLevel = TechConstants.T_IS_ADVANCED;
        this.name = "Small VSP";
        this.setInternalName("ISSmallVSPLaser");
        this.setInternalName("ISSVSPL");
        this.setInternalName("ISSmallVariableSpeedLaser");
        this.heat = 3;
        this.damage = WeaponType.DAMAGE_VARIABLE;
        this.toHitModifier = -4;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.waterShortRange = 2;
        this.waterMediumRange = 3;
        this.waterLongRange = 4;
        this.waterExtremeRange = 6;
        this.damageShort = 5;
        this.damageMedium = 4;
        this.damageLong = 3;
        this.tonnage = 2.0f;
        this.criticals = 1;
        this.bv = 22;
        this.cost = 60000;
        this.shortAV = 4;
        this.maxRange = RANGE_SHORT;
    }
    
}
