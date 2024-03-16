/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org).
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
package megamek.common.weapons.lrms;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class ISExtendedLRM20 extends ExtendedLRMWeapon {
    private static final long serialVersionUID = -2230366483054553162L;

    public ISExtendedLRM20() {
        super();
        name = "Extended LRM 20";
        setInternalName(name);
        addLookupName("IS Extended LRM-20");
        addLookupName("ISExtendedLRM20");
        addLookupName("IS Extended LRM 20");
        addLookupName("ELRM-20 (THB)");
        heat = 10;
        rackSize = 20;
        tonnage = 18.0;
        criticals = 8;
        bv = 268;
        cost = 500000;
        shortAV = 12;
        medAV = 12;
        longAV = 12;
        extAV = 12;
        rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement
                .setTechBase(TECH_BASE_IS)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(DATE_NONE, 3054, 3080, DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted ignore) {
        return range == AlphaStrikeElement.SHORT_RANGE ? 0.3 : 1.2;
    }
}
