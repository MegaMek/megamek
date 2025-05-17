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

import megamek.common.AmmoType;

/**
 * @author Sebastian Brocks
 */
public class ISImprovedNarcOS extends NarcWeapon {
    private static final long serialVersionUID = -3509295242151016719L;

    public ISImprovedNarcOS() {
        super();
        name = "iNarc (OS)";
        setInternalName("ISImprovedNarc (OS)");
        addLookupName("IS OS iNarc Beacon");
        addLookupName("IS iNarc Missile Beacon (OS)");
        sortingName = "Narc X OS";
        ammoType = AmmoType.T_INARC;
        heat = 0;
        rackSize = 1;
        shortRange = 4;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 22;
        tonnage = 5.5;
        criticals = 2;
        bv = 15;
        flags = flags.or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 250000;
        rulesRefs = "232, TM";
        techAdvancement.setTechBase(TechBase.IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(TechRating.E)
            .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
            .setISAdvancement(3054, 3062, 3070, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false, false, false)
            .setPrototypeFactions(Faction.CS)
            .setProductionFactions(Faction.CS, Faction.WB);
    }
}
