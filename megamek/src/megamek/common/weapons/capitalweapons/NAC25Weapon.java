/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 2 of the License, or (at your option) 
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 */
package megamek.common.weapons.capitalweapons;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class NAC25Weapon extends NavalACWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public NAC25Weapon() {
        super();
        this.name = "Naval Autocannon (NAC/25)";
        this.setInternalName(this.name);
        this.addLookupName("NAC25");
        this.shortName = "NAC/25";
        this.heat = 85;
        this.damage = 25;
        this.rackSize = 25;
        this.shortRange = 10;
        this.mediumRange = 20;
        this.longRange = 30;
        this.extremeRange = 40;
        this.tonnage = 3000.0;
        this.bv = 4740;
        this.cost = 7500000;
        this.shortAV = 25;
        this.medAV = 25;
        this.longAV = 25;
        this.maxRange = RANGE_LONG;
        rulesRefs = "331, TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
        	    .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_D)
                .setAvailability(RATING_E, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
                .setISApproximate(false, true, false, true, false)
                .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setProductionFactions(F_TA)
                .setReintroductionFactions(F_FS, F_LC);
    }
}
