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
package megamek.common.weapons.other;

/**
 * @author Sebastian Brocks
 */
public class ISNarcOS extends NarcWeapon {
    private static final long serialVersionUID = 8610275030183400408L;

    public ISNarcOS() {
        super();

        name = "Narc (OS)";
        setInternalName("ISNarcBeacon (OS)");
        addLookupName("IS OS Narc Beacon");
        addLookupName("IS Narc Missile Beacon (OS)");
        heat = 0;
        rackSize = 1;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 3.5;
        criticals = 2;
        flags = flags.or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        bv = 6;
        cost = 100000;
        rulesRefs = "232, TM";
        techAdvancement
                .setTechBase(TechBase.ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(TechRating.E, TechRating.F, TechRating.D, TechRating.C)
                .setISAdvancement(2665, 26767, 3045, 2795, 3035)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2665, 26767, 3045, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.FS, Faction.LC);
    }
}
