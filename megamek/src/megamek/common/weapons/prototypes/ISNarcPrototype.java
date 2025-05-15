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
package megamek.common.weapons.prototypes;

import megamek.common.SimpleTechLevel;
import megamek.common.weapons.other.NarcWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISNarcPrototype extends NarcWeapon {
    private static final long serialVersionUID = 5562345335764812479L;

    public ISNarcPrototype() {
        super();
        // TODO : IO pg 73 - Proto Narcs only give +1 to cluster table
        this.name = "Prototype Narc Missile Beacon";
        this.setInternalName("ISNarcBeaconPrototype");
        this.addLookupName("PrototypeNARCBeacon");
        shortName = "Narc (P)";
        this.heat = 0;
        this.rackSize = 1;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 3.0;
        this.criticals = 2;
        this.bv = 15;
        this.cost = 300000;
        flags = flags.or(F_PROTOTYPE);
        rulesRefs = "71, IO";
        techAdvancement.setTechBase(TechBase.IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(TechRating.F, TechRating.X, TechRating.X, TechRating.X)
                .setISAdvancement(2580, DATE_NONE, DATE_NONE, 2587, DATE_NONE)
                .setISApproximate(false, false, false, true, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
