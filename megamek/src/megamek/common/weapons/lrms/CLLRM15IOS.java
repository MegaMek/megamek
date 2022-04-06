/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
public class CLLRM15IOS extends LRMWeapon {
    private static final long serialVersionUID = 5658731828818701699L;

    public CLLRM15IOS() {
        super();
        name = "LRM 15 (I-OS)";
        setInternalName("CLLRM15 (IOS)");
        addLookupName("Clan IOS LRM-15");
        addLookupName("Clan LRM 15 (IOS)");
        heat = 5;
        rackSize = 15;
        minimumRange = WEAPON_NA;
        tonnage = 3.0;
        criticals = 2;
        bv = 33;
        flags = flags.or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 140000;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        maxRange = RANGE_LONG;
        rulesRefs = "327, TO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3058, 3081, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_CNC)
                .setProductionFactions(F_CNC);
    }
}
