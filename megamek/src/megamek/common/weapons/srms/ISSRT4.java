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
package megamek.common.weapons.srms;

/**
 * @author Sebastian Brocks
 */
public class ISSRT4 extends SRTWeapon {
    private static final long serialVersionUID = -5648326444418700888L;

    public ISSRT4() {
        super();
        this.name = "SRT 4";
        this.setInternalName(this.name);
        this.addLookupName("IS SRT-4");
        this.addLookupName("ISSRT4");
        this.addLookupName("IS SRT 4");
        this.heat = 3;
        this.rackSize = 4;
        this.waterShortRange = 3;
        this.waterMediumRange = 6;
        this.waterLongRange = 9;
        this.waterExtremeRange = 12;
        this.tonnage = 2.0;
        this.criticals = 1;
        this.bv = 39;
        this.cost = 60000;
        rulesRefs = "230, TM";
        techAdvancement.setTechBase(TechBase.IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
                .setISAdvancement(2665, 2676, 3045, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false,false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.FW);
    }
}
