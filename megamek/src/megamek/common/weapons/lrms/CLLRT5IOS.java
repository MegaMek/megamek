/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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
public class CLLRT5IOS extends LRTWeapon {
    private static final long serialVersionUID = -3651580547253252279L;

    public CLLRT5IOS() {
        super();
        name = "LRT 5 (I-OS)";
        setInternalName("CLLRTorpedo5 (IOS)");
        addLookupName("Clan IOS LRT-5");
        addLookupName("Clan LRT 5 (IOS)");
        addLookupName("CLLRT5IOS");
        heat = 2;
        rackSize = 5;
        minimumRange = WEAPON_NA;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 0.5;
        criticals = 1;
        bv = 11;
        flags = flags.or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 24000;
        rulesRefs = "327, TO";
        techAdvancement.setTechBase(TechBase.CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(TechRating.X, TechRating.X, TechRating.F, TechRating.E)
                .setClanAdvancement(3058, 3081, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(Faction.CNC)
                .setProductionFactions(Faction.CNC);
    }
}
