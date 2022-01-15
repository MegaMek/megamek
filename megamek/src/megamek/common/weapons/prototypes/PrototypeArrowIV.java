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

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;
import megamek.common.weapons.artillery.ArtilleryWeapon;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class PrototypeArrowIV extends ArtilleryWeapon {
    private static final long serialVersionUID = -4495524659692575107L;

    public PrototypeArrowIV() {
        super();
        name = "Prototype Arrow IV";
        setInternalName("ProtoTypeArrowIV");
        addLookupName("ProtoArrowIVSystem");
        shortName = "Arrow IV (P)";
        heat = 10;
        rackSize = 20;
        ammoType = AmmoType.T_ARROWIV_PROTO;
        shortRange = 1;
        mediumRange = 2;
        longRange = 8;
        extremeRange = 8; // No extreme range.
        tonnage = 16;
        criticals = 16;
        bv = 240;
        cost = 1800000;
        this.flags = flags.or(F_MISSILE).or(F_PROTOTYPE).or(F_ARTILLERY);
        rulesRefs = "284, TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2593, DATE_NONE, DATE_NONE, 2613, 3044)
                .setISApproximate(false, false, false, true, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_CC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
