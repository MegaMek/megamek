/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.capitalweapons;

import megamek.common.AmmoType;
import megamek.common.TechAdvancement;

/**
 * @author Jay Lawson
 */
public class CLNGaussWeaponMedium extends NavalGaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public CLNGaussWeaponMedium() {
        super();
        this.name = "Medium N-Gauss (Clan)";
        this.setInternalName(this.name);
        this.addLookupName("CLMediumNGauss");
        this.heat = 15;
        this.damage = 25;
        this.ammoType = AmmoType.T_MED_NGAUSS;
        this.shortRange = 13;
        this.mediumRange = 26;
        this.longRange = 39;
        this.extremeRange = 52;
        this.tonnage = 5500.0f;
        this.bv = 5040;
        this.cost = 30350000;
        this.shortAV = 25;
        this.medAV = 25;
        this.longAV = 25;
        this.extAV = 25;
        this.maxRange = RANGE_EXT;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(DATE_NONE, 2820, DATE_NONE);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_E, RATING_E, RATING_X });
    }
}
//Commented out in Weapontype. Tech progression in IS weapon covers these.