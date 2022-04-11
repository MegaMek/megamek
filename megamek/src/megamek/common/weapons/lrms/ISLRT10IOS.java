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

/**
 * @author Sebastian Brocks
 */
public class ISLRT10IOS extends LRTWeapon {
    private static final long serialVersionUID = 2674309948460871883L;

    public ISLRT10IOS() {
        super();

        name = "LRT 10 (I-OS)";
        setInternalName(name);
        addLookupName("IS IOS LRT-10");
        addLookupName("ISLRTorpedo10 (IOS)");
        addLookupName("IS LRT 10 (IOS)");
        addLookupName("ISLRT10IOS");
        heat = 4;
        rackSize = 10;
        minimumRange = 6;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 4.5;
        criticals = 2;
        bv = 18;
        flags = flags.or(F_ONESHOT);
        cost = 80000;
        rulesRefs = "327, TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3056, 3081, 3085, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
    }
}
