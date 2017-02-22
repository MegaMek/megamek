/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
/*
 * Created on Sep 25, 2004
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import megamek.common.TechProgression;

/**
 * @author Andrew Hunter
 */
public class CLImprovedAC10 extends ACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 814114264108820161L;

    /**
     * 
     */
    public CLImprovedAC10() {
        super();

        name = "Improved Autocannon/10";
        setInternalName("Improved Autocannon/10");
        addLookupName("CLIMPAC10");
        heat = 3;
        damage = 10;
        rackSize = 10;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 11.0f;
        criticals = 6;
        bv = 123;
        cost = 200000;
        shortAV = 10;
        medAV = 10;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        ammoType = AmmoType.T_AC_IMP;
        introDate = 2810;
        extinctDate = 2833;
        reintroDate = 3080;
        techLevel.put(2810, TechConstants.T_CLAN_ADVANCED);   ///ADV
        techLevel.put(2818, TechConstants.T_CLAN_TW);   ///COMMON
        availRating = new int[] { RATING_X, RATING_C, RATING_X, RATING_X };
        techRating = RATING_E;
        rulesRefs = "96, IO";
        techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        techProgression.setClanProgression(DATE_NONE, 2810, 2818, 2833, 3080);
        techProgression.setTechRating(RATING_E);
        techProgression.setAvailability( new int[] { RATING_X, RATING_C, RATING_X, RATING_X });
    }
}
