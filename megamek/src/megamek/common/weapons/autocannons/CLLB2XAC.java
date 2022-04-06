/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.autocannons;

/**
 * @author Andrew Hunter
 * @since Oct 15, 2004
 */
public class CLLB2XAC extends LBXACWeapon {
    private static final long serialVersionUID = -2333780992130250932L;

    public CLLB2XAC() {
        super();
        name = "LB 2-X AC";
        setInternalName("CLLBXAC2");
        addLookupName("Clan LB 2-X AC");
        sortingName = "LB 02-X AC";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 4;
        shortRange = 10;
        mediumRange = 20;
        longRange = 30;
        extremeRange = 40;
        tonnage = 5.0;
        criticals = 3;
        bv = 47;
        cost = 150000;
        shortAV = 2;
        medAV = 2;
        longAV = 2;
        extAV = 2;
        maxRange = RANGE_EXT;
        rulesRefs = "207, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        .setIntroLevel(false)
        .setTechRating(RATING_F)
        .setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
        .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, true, false, false, false)
        .setProductionFactions(F_CCY)
        .setReintroductionFactions(F_CGS);
    }
}
