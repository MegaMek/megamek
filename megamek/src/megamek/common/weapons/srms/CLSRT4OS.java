/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.srms;

/**
 * @author Sebastian Brocks
 */
public class CLSRT4OS extends SRTWeapon {
    private static final long serialVersionUID = -1622623253640255529L;

    public CLSRT4OS() {
        super();
        name = "SRT 4 (OS)";
        setInternalName("CLSRT4 (OS)");
        addLookupName("Clan OS SRT-4");
        addLookupName("Clan SRT 4 (OS)");
        addLookupName("CLSRT4OS");
        heat = 3;
        rackSize = 4;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 1.5;
        criticals = 1;
        bv = 8;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 30000;
        rulesRefs = "230, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setClanAdvancement(2665, 2676, 3045, 2800, 3030)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_FW);
    }
}
