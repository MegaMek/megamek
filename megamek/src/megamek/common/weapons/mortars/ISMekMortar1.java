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
package megamek.common.weapons.mortars;

/**
 * @author Jason Tighe
 */
public class ISMekMortar1 extends MekMortarWeapon {
    private static final long serialVersionUID = -5651886218762631122L;

    public ISMekMortar1() {
        super();

        name = "'Mech Mortar 1";
        setInternalName("IS Mech Mortar-1");
        addLookupName("ISMekMortar1");
        addLookupName("IS Mek Mortar 1");
        rackSize = 1;
        minimumRange = 6;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        bv = 10;
        heat = 1;
        criticals = 1;
        tonnage = 2;
        cost = 7000;
        rulesRefs = "324, TO";
        techAdvancement.setTechBase(TechBase.ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E)
                .setISAdvancement(2526, 2531, 3052, 2819, 3043)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2526, 2531, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.FS, Faction.LC);
    }
}
