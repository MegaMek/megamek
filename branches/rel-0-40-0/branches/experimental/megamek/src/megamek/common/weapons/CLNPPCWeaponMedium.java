/*
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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Jay Lawson
 */
public class CLNPPCWeaponMedium extends NavalPPCWeapon {
    /**
    * 
    */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
    * 
    */
    public CLNPPCWeaponMedium() {
        super();
        this.techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        this.name = "Medium NPPC (Clan)";
        this.setInternalName(this.name);
        this.addLookupName("CLMediumNPPC");
        this.heat = 135;
        this.damage = 9;
        this.shortRange = 12;
        this.mediumRange = 24;
        this.longRange = 36;
        this.extremeRange = 48;
        this.tonnage = 1800.0f;
        this.bv = 2268;
        this.cost = 3250000;
        this.shortAV = 9;
        this.medAV = 9;
        this.longAV = 9;
        this.extAV = 9;
        this.maxRange = RANGE_EXT;
        introDate = 2820;
        techLevel.put(2820, techLevel.get(3071));;
        availRating = new int[] { RATING_D, RATING_F, RATING_D };
        techRating = RATING_D;

    }
}