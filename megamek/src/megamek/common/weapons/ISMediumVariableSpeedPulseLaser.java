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
 * @author Jason Tighe
 */
public class ISMediumVariableSpeedPulseLaser extends VariableSpeedPulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 2676144961105838316L;

    /**
     * 
     */
    public ISMediumVariableSpeedPulseLaser() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Medium VSP";
        this.setInternalName("ISMediumVSPLaser");
        this.setInternalName("ISMVSPL");
        this.setInternalName("ISMediumVariableSpeedLaser");
        this.heat = 7;
        this.damage = DAMAGE_VARIABLE;
        this.toHitModifier = -4;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.waterShortRange = 2;
        this.waterMediumRange = 3;
        this.waterLongRange = 4;
        this.waterExtremeRange = 6;
        this.damageShort = 9;
        this.damageMedium = 7;
        this.damageLong = 5;
        this.tonnage = 4.0f;
        this.criticals = 2;
        this.bv = 56;
        this.cost = 20000;
        this.shortAV = 7;
        this.maxRange = RANGE_SHORT;
    }
    
}
