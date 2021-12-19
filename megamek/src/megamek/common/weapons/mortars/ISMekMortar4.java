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
public class ISMekMortar4 extends MekMortarWeapon {
    private static final long serialVersionUID = 6803604562717710451L;

    public ISMekMortar4() {
        super();

        name = "'Mech Mortar 4";
        setInternalName("IS Mech Mortar-4");
        addLookupName("ISMekMortar4");
        addLookupName("IS Mek Mortar 4");
        rackSize = 4;
        minimumRange = 6;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        bv = 26;
        heat = 5;
        criticals = 3;
        tonnage = 7;
        cost = 32000;
        rulesRefs = "324, TO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_B)
            .setAvailability(RATING_D, RATING_F, RATING_F, RATING_E)
            .setISAdvancement(2526, 2531, 3052, 2819, 3043)
            .setISApproximate(true, false, false, false, false)
            .setClanAdvancement(2526, 2531, DATE_NONE, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, false, false, false, false)
            .setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH)
            .setReintroductionFactions(F_FS,F_LC);
    }
}
