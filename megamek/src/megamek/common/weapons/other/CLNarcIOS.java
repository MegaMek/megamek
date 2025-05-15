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
public class CLNarcIOS extends NarcWeapon {
    private static final long serialVersionUID = 5553288957570246232L;

    public CLNarcIOS() {
        super();

        name = "Narc (I-OS)";
        setInternalName("CLNarcBeacon (I-OS)");
        addLookupName("Clan I-OS Narc Beacon");
        addLookupName("Clan Narc Missile Beacon (I-OS)");
        heat = 0;
        rackSize = 1;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 1.5;
        criticals = 1;
        flags = flags.or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        bv = 6;
        cost = 100000;
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
