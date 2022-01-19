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
public class CLBAMGHeavy extends BAMGWeapon {
    private static final long serialVersionUID = 7184744610192773285L;

    public CLBAMGHeavy() {
        super();
        name = "Machine Gun (Heavy)";
        setInternalName("CLBAHeavyMG");
        addLookupName("Clan BA Heavy Machine Gun");
        addLookupName("ISBAHeavyMachineGun");
        addLookupName("IS BA Heavy Machine Gun");
        addLookupName("ISBAHeavyMG");
        sortingName = "MG D";
        heat = 0;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        rackSize = 3;
        shortRange = 1;
        mediumRange = 2;
        longRange = 2;
        extremeRange = 2;
        tonnage = 0.15;
        criticals = 1;
        bv = 6;
        cost = 7500;
        rulesRefs = "258,TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_C, RATING_B)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3068, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(3055, 3059, 3068, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
    }
}
