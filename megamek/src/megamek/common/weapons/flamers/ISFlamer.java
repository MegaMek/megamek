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
package megamek.common.weapons.flamers;

import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 * @since Sep 24, 2004
 */
public class ISFlamer extends FlamerWeapon {
    private static final long serialVersionUID = 1414639280093120062L;

    public ISFlamer() {
        super();
        name = "Flamer";
        setInternalName(this.name);
        addLookupName("IS Flamer");
        addLookupName("ISFlamer");
        sortingName = "Flamer C";
        heat = 3;
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_4D6;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 1;
        criticals = 1;
        bv = 6;
        cost = 7500;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        rulesRefs = "218, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(true)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_A)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, 2830, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
    }
}
