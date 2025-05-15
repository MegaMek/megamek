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

import megamek.common.*;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class CLArrowIV extends ArrowIV {

    public CLArrowIV() {
        super();

        setInternalName("CLArrowIV");
        addLookupName("CLArrowIVSystem");
        addLookupName("Clan Arrow IV System");
        addLookupName("Clan Arrow IV Missile System");
        heat = 10;
        rackSize = 20;
        ammoType = AmmoType.T_ARROW_IV;
        shortRange = 1; //
        mediumRange = 2;
        longRange = 9;
        extremeRange = 9; // No extreme range.
        tonnage = 12;
        criticals = 12;
        svslots = 6;
        techAdvancement.setTechBase(TechBase.CLAN)
                .setTechRating(TechRating.F)
                .setAvailability(TechRating.X, TechRating.F, TechRating.E, TechRating.D)
                .setClanAdvancement(DATE_NONE, 2844, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CHH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
}
