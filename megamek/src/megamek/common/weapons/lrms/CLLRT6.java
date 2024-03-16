/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org).
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
package megamek.common.weapons.lrms;

/**
 * @author Sebastian Brocks
 */
public class CLLRT6 extends LRTWeapon {
    private static final long serialVersionUID = 8809381027508149200L;

    public CLLRT6() {
        super();

        this.name = "LRT 6";
        this.setInternalName("CLLRTorpedo6");
        this.setInternalName("CLLRT6");
        this.heat = 0;
        this.rackSize = 6;
        this.minimumRange = WEAPON_NA;
        this.waterShortRange = 7;
        this.waterMediumRange = 14;
        this.waterLongRange = 21;
        this.waterExtremeRange = 28;
        this.tonnage = 1.2;
        this.criticals = 0;
        this.bv = 69;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression. 
        // But LRM Tech Base and Avail Ratings.
        rulesRefs = "231, TM";
        flags = flags.or(F_NO_FIRES).andNot(F_AERO_WEAPON).andNot(F_BA_WEAPON)
                .andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
    }
}
