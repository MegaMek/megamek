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
package megamek.common.weapons.srms;

/**
 * @author Sebastian Brocks
 */
public class ISSRT4OS extends SRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 1994847810524115314L;

    /**
     *
     */
    public ISSRT4OS() {
        super();
        name = "SRT 4 (OS)";
        setInternalName("ISSRT4OS");
        addLookupName("ISSRT4 (OS)"); // mtf
        addLookupName("IS SRT 4 (OS)"); // tdb
        addLookupName("OS SRT-4"); // mep
        heat = 3;
        rackSize = 4;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 2.5;
        criticals = 1;
        bv = 8;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 30000;
        rulesRefs = "230,TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_C)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setISAdvancement(2665, 2676, 3045, 2800, 3030)
            .setISApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH)
            .setReintroductionFactions(F_FW);
    }
}
