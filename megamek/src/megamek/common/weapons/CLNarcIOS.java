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
public class CLNarcIOS extends NarcWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5553288957570246232L;

    /**
     *
     */
    public CLNarcIOS() {
        super();

        name = "Narc (I-OS)";
        setInternalName("CLNarcBeacon (I-OS)");
        addLookupName("Clan I-OS Narc Beacon");
        addLookupName("Clan Narc Missile Beacon (I-OS)");
        heat = 0;
        rackSize = 1;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 1.5f;
        criticals = 1;
        flags = flags.or(F_ONESHOT);
        bv = 6;
        cost = 100000;
        introDate = 3058;
        techLevel.put(3058, TechConstants.T_CLAN_EXPERIMENTAL);
        techLevel.put(3081, TechConstants.T_CLAN_ADVANCED);
        techLevel.put(3085, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X,RATING_E ,RATING_D ,RATING_C};
        techRating = RATING_F;
        rulesRefs = "232, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(3058, 3081, 3085);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_E, RATING_D, RATING_C });
    }
}
