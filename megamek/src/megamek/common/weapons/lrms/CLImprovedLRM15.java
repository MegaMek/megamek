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
package megamek.common.weapons.lrms;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class CLImprovedLRM15 extends LRMWeapon {

    private static final long serialVersionUID = 603060073432118270L;

    public CLImprovedLRM15() {
        super();
        name = "Improved LRM 15";
        setInternalName(name);
        addLookupName("CLImprovedLRM15");
        addLookupName("CLImpLRM15");
        heat = 5;
        rackSize = 15;
        minimumRange = 6;
        tonnage = 3.5;
        criticals = 2;
        bv = 136;
        cost = 175000;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        maxRange = RANGE_LONG;
        ammoType = AmmoType.T_LRM_IMP;
        rulesRefs = "96, IO";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
            .setClanAdvancement(2815, 2818, 2820, 2831, 3080)
            .setPrototypeFactions(F_CCY).setProductionFactions(F_CCY)
            .setReintroductionFactions(F_EI).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public String getSortingName() {
        // revert LRMWeapon's override here as the name is not just "LRM xx"
        return name;
    }
}
