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
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISSnubNosePPC extends PPCWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -5650794792475465261L;

    /**
     * 
     */
    public ISSnubNosePPC() {
        super();

        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Snub-Nose PPC";
        this.setInternalName("ISSNPPC");
        this.addLookupName("ISSnubNosedPPC");
        this.heat = 10;
        this.damage = DAMAGE_VARIABLE;
        this.minimumRange = 0;
        this.shortRange = 9;
        this.mediumRange = 13;
        this.longRange = 15;
        this.extremeRange = 22;
        this.waterShortRange = 6;
        this.waterMediumRange = 8;
        this.waterLongRange = 9;
        this.waterExtremeRange = 13;
        this.damageShort = 10;
        this.damageMedium = 8;
        this.damageLong = 5;
        this.tonnage = 6.0f;
        this.criticals = 2;
        this.bv = 165;
        this.cost = 400000;
        this.maxRange = RANGE_MED;
        this.shortAV = 10;
        this.medAV = 8;
    }

    public int getDamage(int range) {
        if ( range <= shortRange )
            return damageShort;
        
        if ( range <= mediumRange )
            return damageMedium;
        
            return damageLong;
    }

}
