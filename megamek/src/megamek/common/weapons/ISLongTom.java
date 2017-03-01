/**
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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class ISLongTom extends ArtilleryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5323886711682442495L;

    /**
     *
     */
    public ISLongTom() {
        super();
        name = "Long Tom";
        setInternalName("ISLongTom");
        addLookupName("ISLongTomArtillery");
        addLookupName("IS Long Tom");
        heat = 20;
        rackSize = 25;
        ammoType = AmmoType.T_LONG_TOM;
        shortRange = 1;
        mediumRange = 2;
        longRange = 30;
        extremeRange = 30; // No extreme range.
        tonnage = 30f;
        criticals = 30;
        bv = 368;
        cost = 450000;
        rulesRefs = "284, TO";

        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2445, 2500, DATE_NONE);
        techAdvancement.setTechRating(RATING_B);
        techAdvancement.setAvailability( new int[] { RATING_C, RATING_C, RATING_C, RATING_C });
    }

}
