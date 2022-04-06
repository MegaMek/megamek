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
public class NPPCWeaponMedium extends NPPCWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public NPPCWeaponMedium() {
        super();
        name = "Naval PPC (Medium)";
        setInternalName(this.name);
        addLookupName("MediumNPPC");
        addLookupName("Medium NPPC (Clan)");
        shortName = "Medium NPPC";
        sortingName = "PPC Naval C";
        heat = 135;
        damage = 9;
        shortRange = 12;
        mediumRange = 24;
        longRange = 36;
        extremeRange = 48;
        tonnage = 1800.0;
        bv = 2268;
        cost = 3250000;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        extAV = 9;
        maxRange = RANGE_EXT;
        rulesRefs = "333, TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_D)
                .setAvailability(RATING_D, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(2350, 2356, DATE_NONE, 2950, 3052)
                .setISApproximate(true, true, false, true, false)
                .setClanAdvancement(2350, 2356, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_DC);
    }
}
