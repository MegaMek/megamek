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

/**
 * @author Jay Lawson
 */
public class NGaussWeaponLight extends NGaussWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public NGaussWeaponLight() {
        super();
        this.name = "Naval Gauss (Light)";
        this.setInternalName(this.name);
        this.addLookupName("LightNGauss");
        this.addLookupName("CLLightNGauss");
        this.addLookupName("Light N-Gauss (Clan)");
        this.shortName = "Light NGauss";
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
        rulesRefs = "333,TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_E, RATING_X, RATING_E, RATING_X)
            .setISAdvancement(2440, 2448, DATE_NONE, 2950, 3052)
            .setISApproximate(true, true, false, true, false)
            .setClanAdvancement(2440, 2448, DATE_NONE, DATE_NONE, DATE_NONE)
            .setClanApproximate(true, true, false, false, false)
            .setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH)
            .setReintroductionFactions(F_DC);
    }
}
