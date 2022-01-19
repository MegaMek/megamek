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

import megamek.common.AmmoType;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class NGaussWeaponMedium extends NGaussWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public NGaussWeaponMedium() {
        super();
        name = "Naval Gauss (Medium)";
        setInternalName(this.name);
        addLookupName("MediumNGauss");
        addLookupName("CLMediumNGauss");
        addLookupName("Medium N-Gauss (Clan)");
        shortName = "Medium NGauss";
        sortingName = "Gauss Naval C";
        heat = 15;
        damage = 25;
        ammoType = AmmoType.T_MED_NGAUSS;
        shortRange = 13;
        mediumRange = 26;
        longRange = 39;
        extremeRange = 52;
        tonnage = 5500;
        bv = 5040;
        cost = 30350000;
        shortAV = 25;
        medAV = 25;
        longAV = 25;
        extAV = 25;
        maxRange = RANGE_EXT;
        rulesRefs = "333, TO";
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
