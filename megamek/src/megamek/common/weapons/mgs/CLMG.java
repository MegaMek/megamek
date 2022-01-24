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
package megamek.common.weapons.mgs;

import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 * @since Oct 20, 2004
 */
public class CLMG extends MGWeapon {
    private static final long serialVersionUID = 2557643305248678454L;

    public CLMG() {
        super();
        name = "Machine Gun";
        setInternalName("CLMG");
        addLookupName("Clan Machine Gun");
        sortingName = "MG C";
        heat = 0;
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        rackSize = 2;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.25;
        criticals = 1;
        bv = 5;
        cost = 5000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        rulesRefs = "228, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_B, RATING_B, RATING_A)
                .setClanAdvancement(2821, 2825, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
    }
}
