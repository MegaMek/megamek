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
public class CLSRT5OS extends SRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 9051765359928076836L;

    /**
     *
     */
    public CLSRT5OS() {
        super();

        name = "SRT 5 OS";
        setInternalName("CLSRT5OS");
        addLookupName("Clan SRT-5 OS");
        addLookupName("Clan SRT 5 OS");
        rackSize = 5;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        bv = 9;
        tonnage = 1.25;
        criticals = 1;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 80000;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression. 
        //But LRM Tech Base and Avail Ratings.
        rulesRefs = "230,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_F)
        .setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
        .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false,false, false)
        .setPrototypeFactions(F_CSJ)
        .setProductionFactions(F_CSJ);
    }
}
