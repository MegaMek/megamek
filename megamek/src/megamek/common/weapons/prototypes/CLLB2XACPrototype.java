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
package megamek.common.weapons.prototypes;

import megamek.common.SimpleTechLevel;

/**
 * @author Andrew Hunter
 * @since Oct 15, 2004
 */
public class CLLB2XACPrototype extends CLLBXACPrototypeWeapon {
    private static final long serialVersionUID = 3580100820831141313L;

    public CLLB2XACPrototype() {
        super();
        name = "Prototype LB 2-X Autocannon";
        setInternalName("CLLBXAC2Prototype");
        shortName = "LB 2-X (P)";
        sortingName = "Prototype LB 02-X Autocannon";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 4;
        shortRange = 9;
        mediumRange = 18;
        longRange = 27;
        extremeRange = 36;
        tonnage = 6.0;
        criticals = 5;
        bv = 42;
        cost = 150000;
        shortAV = 2;
        medAV = 2;
        longAV = 2;
        extAV = 2;
        maxRange = RANGE_EXT;
        rulesRefs = "97, IO";
        flags = flags.or(F_PROTOTYPE).andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2820, DATE_NONE, DATE_NONE, 2826, DATE_NONE)
                .setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(F_CGS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
