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
package megamek.common.weapons.battlearmor;

import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class CLBAMGLight extends BAMGWeapon {
    private static final long serialVersionUID = 1810341654439496432L;

    public CLBAMGLight() {
        super();
        name = "Machine Gun (Light)";
        setInternalName("CLBALightMG");
        addLookupName("Clan BA Light Machine Gun");
        addLookupName("ISBALightMachineGun");
        addLookupName("IS BA Light Machine Gun");
        addLookupName("ISBALightMG");
        sortingName = "MG B";
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_HALFD6;
        rackSize = 1;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        tonnage = 0.075;
        criticals = 1;
        bv = 5;
        cost = 5000;
        rulesRefs = "258, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_C, RATING_B)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3068, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(3055, 3060, 3068, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
    }
}
