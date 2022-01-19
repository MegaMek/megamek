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
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class ISHeavyMG extends MGWeapon {
    private static final long serialVersionUID = 9170779598178306077L;

    public ISHeavyMG() {
        super();

        name = "Heavy Machine Gun";
        setInternalName(name);
        addLookupName("IS Heavy Machine Gun");
        addLookupName("ISHeavyMG");
        sortingName = "MG D";
        ammoType = AmmoType.T_MG_HEAVY;
        heat = 0;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_3D6;
        rackSize = 3;
        shortRange = 1;
        mediumRange = 2;
        longRange = 2;
        extremeRange = 2;
        tonnage = 1.0;
        criticals = 1;
        bv = 6;
        cost = 7500;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        rulesRefs = "228, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_C, RATING_C, RATING_B)
                .setISAdvancement(3063, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TC)
                .setProductionFactions(F_TC);
    }
}
