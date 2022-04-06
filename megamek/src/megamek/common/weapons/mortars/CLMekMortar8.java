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
public class CLMekMortar8 extends MekMortarWeapon {
    private static final long serialVersionUID = 7757701625628311696L;

    public CLMekMortar8() {
        super();

        name = "'Mech Mortar 8";
        setInternalName("Clan Mech Mortar-8");
        addLookupName("CLMekMortar8");
        addLookupName("Clan Mek Mortar 8");
        rackSize = 8;
        minimumRange = 6;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        bv = 50;
        heat = 10;
        criticals = 3;
        tonnage = 5;
        cost = 70000;
        rulesRefs = "324, TO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_B)
                .setAvailability(RATING_D, RATING_F, RATING_E, RATING_E)
                .setClanAdvancement(2835, 2840, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CBR)
                .setProductionFactions(F_CBR);
    }
}
