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
public class NPPCWeaponHeavy extends NPPCWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public NPPCWeaponHeavy() {
        super();
        name = "Naval PPC (Heavy)";
        setInternalName(this.name);
        addLookupName("HeavyNPPC");
        addLookupName("Heavy NPPC (Clan)");
        shortName = "Heavy NPPC";
        sortingName = "PPC Naval D";
        heat = 225;
        damage = 15;
        shortRange = 13;
        mediumRange = 26;
        longRange = 39;
        extremeRange = 52;
        tonnage = 3000.0;
        bv = 3780;
        cost = 9050000;
        shortAV = 15;
        medAV = 15;
        longAV = 15;
        extAV = 15;
        maxRange = RANGE_EXT;
        rulesRefs = "333, TO";
        techAdvancement.setTechBase(TechBase.ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(2350, 2356, DATE_NONE, 2950, 3052)
                .setISApproximate(true, true, false, true, false)
                .setClanAdvancement(2350, 2356, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.DC);
    }
}
