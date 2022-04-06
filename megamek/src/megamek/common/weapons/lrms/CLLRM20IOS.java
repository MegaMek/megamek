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
public class CLLRM20IOS extends LRMWeapon {
    private static final long serialVersionUID = 1725628953852049901L;

    public CLLRM20IOS() {
        super();
        name = "LRM 20 (I-OS)";
        setInternalName("CLLRM20 (IOS)");
        addLookupName("Clan IOS LRM-20");
        addLookupName("Clan LRM 20 (IOS)");
        heat = 6;
        rackSize = 20;
        minimumRange = WEAPON_NA;
        tonnage = 4.5;
        criticals = 4;
        bv = 44;
        flags = flags.or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 200000;
        shortAV = 12;
        medAV = 12;
        longAV = 12;
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
