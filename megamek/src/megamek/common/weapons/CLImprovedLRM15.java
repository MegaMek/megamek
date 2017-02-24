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

import megamek.common.AmmoType;
import megamek.common.TechConstants;
import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class CLImprovedLRM15 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 603060073432118270L;

    /**
     *
     */
    public CLImprovedLRM15() {
        super();

        name = "Improved LRM 15";
        setInternalName(name);
        addLookupName("CLImprovedLRM15");
        addLookupName("CLImpLRM15");
        heat = 5;
        rackSize = 15;
        minimumRange = 6;
        tonnage = 3.5f;
        criticals = 2;
        bv = 136;
        cost = 175000;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        maxRange = RANGE_LONG;
        ammoType = AmmoType.T_LRM_IMP;
        introDate = 2815;
        extinctDate = 2831;
        reintroDate = 3080;
        techLevel.put(2815, TechConstants.T_CLAN_EXPERIMENTAL);   ///EXP
        techLevel.put(2471, TechConstants.T_CLAN_ADVANCED);   ///ADV
        techLevel.put(2500, TechConstants.T_CLAN_TW);   ///COMMON
        availRating = new int[] { RATING_X, RATING_D, RATING_X, RATING_X };
        techRating = RATING_F;
        rulesRefs = "96, IO";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(2815, DATE_NONE, DATE_NONE, 2831, 3080);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_D, RATING_X, RATING_X });
    }
}
