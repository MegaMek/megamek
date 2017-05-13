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
import megamek.common.weapons.NavalGaussWeapon;

/**
 * @author Jay Lawson
 */
public class CLNGaussWeaponLight extends NavalGaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public CLNGaussWeaponLight() {
        super();
        this.name = "Light N-Gauss (Clan)";
        this.setInternalName(this.name);
        this.addLookupName("CLLightNGauss");
        this.heat = 9;
        this.damage = 15;
        this.ammoType = AmmoType.T_LIGHT_NGAUSS;
        this.shortRange = 14;
        this.mediumRange = 28;
        this.longRange = 40;
        this.extremeRange = 56;
        this.tonnage = 4500.0f;
        this.bv = 3024;
        this.cost = 20300000;
        this.shortAV = 15;
        this.medAV = 15;
        this.longAV = 15;
        this.extAV = 15;
        this.maxRange = RANGE_EXT;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(DATE_NONE, 2820, DATE_NONE);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_E, RATING_E, RATING_X });
    }
}
//Commented out in Weapontype. Tech progression in IS weapon covers these.