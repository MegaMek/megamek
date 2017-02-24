/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.TechConstants;
import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class ISNarcOS extends NarcWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 8610275030183400408L;

    /**
     *
     */
    public ISNarcOS() {
        super();

        name = "Narc (OS)";
        setInternalName("ISNarcBeacon (OS)");
        addLookupName("IS OS Narc Beacon");
        addLookupName("IS Narc Missile Beacon (OS)");
        heat = 0;
        rackSize = 1;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 3.5f;
        criticals = 2;
        flags = flags.or(F_ONESHOT);
        bv = 6;
        cost = 100000;
        introDate = 2660;
        extinctDate = 2795;
        reintroDate = 3049;
        techLevel.put(2660, TechConstants.T_IS_ADVANCED);
        techLevel.put(3055, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_E ,RATING_F ,RATING_D ,RATING_C};
        techRating = RATING_E;
        rulesRefs = "232, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(DATE_NONE, 2660, 3055, 2795, 3049);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_E, RATING_F, RATING_D, RATING_C });
    }
}
