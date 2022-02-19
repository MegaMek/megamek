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

import megamek.common.AmmoType;
import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 * @since Oct 20, 2004
 */
public class CLLightMG extends MGWeapon {
    private static final long serialVersionUID = 4070411380374344272L;

    public CLLightMG() {
        super();
        name = "Light Machine Gun";
        setInternalName("CLLightMG");
        addLookupName("Clan Light Machine Gun");
        sortingName = "MG B";
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        rackSize = 1;
        ammoType = AmmoType.T_MG_LIGHT;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        tonnage = 0.25;
        criticals = 1;
        bv = 5;
        cost = 5000;
        shortAV = 1;
        maxRange = RANGE_SHORT;
        atClass = CLASS_AC;
        rulesRefs = "228, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_C, RATING_C, RATING_B)
                .setClanAdvancement(3055, 3060, 3070, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
    }
}
