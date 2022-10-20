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

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Mounted;

import megamek.common.SimpleTechLevel;

/**
 * @author Andrew Hunter
 * @since Oct 15, 2004
 */
public class CLLB5XACPrototype extends CLLBXACPrototypeWeapon {
    private static final long serialVersionUID = -8003492051812171922L;

    public CLLB5XACPrototype() {
        super();
        name = "Prototype LB 5-X Autocannon";
        setInternalName("CLLBXAC5Prototype");
        shortName = "LB 5-X (P)";
        sortingName = "Prototype LB 05-X Autocannon";
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 3;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 8.0;
        criticals = 6;
        bv = 83;
        cost = 250000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        flags = flags.or(F_PROTOTYPE).andNot(F_PROTO_WEAPON);
        maxRange = RANGE_LONG;
        rulesRefs = "97, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2820, DATE_NONE, DATE_NONE, 2825, DATE_NONE)
                .setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(F_CCY)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.236;
        } else if (range <= AlphaStrikeElement.LONG_RANGE) {
            return 0.3;
        } else {
            return 0;
        }
    }
}
