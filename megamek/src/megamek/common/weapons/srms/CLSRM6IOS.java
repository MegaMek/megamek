/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons.srms;

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class CLSRM6IOS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5184043200202465163L;

    /**
     *
     */
    public CLSRM6IOS() {
        super();
        name = "SRM 6 (I-OS)";
        setInternalName("CLSRM6 (IOS)");
        addLookupName("Clan IOS SRM-6");
        addLookupName("Clan SRM 6 (IOS)");
        heat = 4;
        rackSize = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 1.0;
        criticals = 1;
        bv = 12;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 64000;
        shortAV = 8;
        maxRange = RANGE_SHORT;
        rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_B)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setClanAdvancement(DATE_NONE, 3058, 3081, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, false, true, false, false)
            .setPrototypeFactions(F_CNC)
            .setProductionFactions(F_CNC)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
