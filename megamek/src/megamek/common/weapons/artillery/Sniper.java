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
package megamek.common.weapons.artillery;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class Sniper extends ArtilleryWeapon {
    private static final long serialVersionUID = -5022670163785084036L;

    public Sniper() {
        super();

        name = "Sniper";
        setInternalName("ISSniper");
        addLookupName("ISSniperArtillery");
        addLookupName("IS Sniper");
        addLookupName("CLSniper");
        addLookupName("CLSniperArtillery");
        addLookupName("Clan Sniper");
        flags = flags.or(F_AERO_WEAPON);
        heat = 10;
        rackSize = 20;
        ammoType = AmmoType.T_SNIPER;
        shortRange = 1;
        mediumRange = 2;
        longRange = 18;
        extremeRange = 18; // No extreme range.
        tonnage = 20;
        criticals = 20;
        svslots = 10;
        bv = 85;
        cost = 300000;
        rulesRefs = "284,TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
            .setTechRating(RATING_B).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
            .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
}
