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
public class ISNarc extends NarcWeapon {
    private static final long serialVersionUID = 5562345335764812479L;

    public ISNarc() {
        super();

        this.name = "Narc";
        this.setInternalName("ISNarcBeacon");
        this.addLookupName("IS Narc Beacon");
        this.addLookupName("IS Narc Missile Beacon");
        this.heat = 0;
        this.rackSize = 1;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 3.0;
        this.criticals = 2;
        this.bv = 30;
        this.cost = 100000;
        rulesRefs = "232, TM";
        techAdvancement.setTechBase(TechBase.ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setISAdvancement(2580, 2587, 3049, 2795, 3035)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2580, 2587, 3049, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.FW);
    }
}
