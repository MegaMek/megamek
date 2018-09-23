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
package megamek.common.weapons.lrms;

/**
 * @author Sebastian Brocks
 */
public class CLLRT20OS extends LRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 4540170151130434608L;

    /**
     *
     */
    public CLLRT20OS() {
        super();
        name = "LRT 20 (OS)";
        setInternalName("CLLRTorpedo20 (OS)");
        addLookupName("Clan OS LRT-20");
        addLookupName("Clan LRT 20 (OS)");
        addLookupName("CLLRT20OS");
        heat = 6;
        rackSize = 20;
        minimumRange = WEAPON_NA;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 5.5;
        criticals = 4;
        bv = 44;
        flags = flags.or(F_ONESHOT);
        cost = 125000;
        rulesRefs = "230,TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_C)
            .setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
            .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
            .setClanApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_CSF)
            .setProductionFactions(F_CSF);
    }
}
