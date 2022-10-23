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
public class CLLB20XACPrototype extends CLLBXACPrototypeWeapon {
    private static final long serialVersionUID = -4257248228202258750L;

    public CLLB20XACPrototype() {
        super();
        name = "Prototype LB 20-X Autocannon";
        setInternalName("CLLBXAC20Prototype");
        shortName = "LB 20-X (P)";
        heat = 6;
        damage = 20;
        rackSize = 20;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 14.0;
        criticals = 12;
        bv = 237;
        cost = 600000;
        shortAV = 20;
        medAV = 20;
        maxRange = RANGE_MED;
        rulesRefs = "97, IO";
        flags = flags.or(F_PROTOTYPE).andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2820, DATE_NONE, DATE_NONE, 2826, DATE_NONE)
                .setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(F_CHH)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 1.26 : 0;
    }
}
