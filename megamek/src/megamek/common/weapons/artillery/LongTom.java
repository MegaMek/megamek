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
public class LongTom extends ArtilleryWeapon {
    private static final long serialVersionUID = 5323886711682442495L;

    public LongTom() {
        super();
        name = "Long Tom";
        setInternalName("ISLongTom");
        addLookupName("ISLongTomArtillery");
        addLookupName("IS Long Tom");
        addLookupName("CLLongTom");
        addLookupName("CLLongTomArtillery");
        addLookupName("Clan Long Tom");
        heat = 20;
        rackSize = 25;
        ammoType = AmmoType.T_LONG_TOM;
        shortRange = 1;
        mediumRange = 2;
        longRange = 30;
        extremeRange = 30; // No extreme range.
        tonnage = 30;
        criticals = 30;
        svslots = 15;
        bv = 368;
        cost = 450000;
        rulesRefs = "284, TO";
        techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_B)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setAdvancement(2445, 2500, DATE_NONE, DATE_NONE, DATE_NONE)
            .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
}
