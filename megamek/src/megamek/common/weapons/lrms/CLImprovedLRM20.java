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
public class CLImprovedLRM20 extends LRMWeapon {

    private static final long serialVersionUID = 3287950524687857609L;

    public CLImprovedLRM20() {
        super();
        name = "Improved LRM 20";
        setInternalName(name);
        addLookupName("CLImprovedLRM20");
        addLookupName("CLImpLRM20");
        heat = 6;
        rackSize = 20;
        minimumRange = 6;
        tonnage = 5.0;
        criticals = 4;
        bv = 181;
        cost = 250000;
        shortAV = 12;
        medAV = 12;
        longAV = 12;
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
