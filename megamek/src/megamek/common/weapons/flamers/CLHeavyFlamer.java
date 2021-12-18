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
 * Created on Sep 23, 2011
 */
package megamek.common.weapons.flamers;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;
import megamek.common.WeaponType;

/**
 * @author Klaus Mittag (based on ISHeavyFlamer by Andrew Hunter)
 */
public class CLHeavyFlamer extends VehicleFlamerWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -3957472644909347725L;

    /**
     *
     */
    public CLHeavyFlamer() {
        super();

        name = "Heavy Flamer";
        setInternalName("CLHeavyFlamer");
        addLookupName("Clan Heavy Flamer");
        addLookupName("CL Heavy Flamer");
        addLookupName("CLHeavyFlamer");
        heat = 5;
        damage = 4;
        infDamageClass = WeaponType.WEAPON_BURST_6D6;
        rackSize = 2;
        ammoType = AmmoType.T_HEAVY_FLAMER;
        shortRange = 2;
        mediumRange = 3;
        longRange = 4;
        extremeRange = 6;
        tonnage = 1.5;
        criticals = 1;
        bv = 15;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        cost = 11250;
        rulesRefs = "312,TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_CLAN)
    	.setIntroLevel(false)
    	.setUnofficial(false)
    	.setTechRating(RATING_C)
    	.setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
        .setClanAdvancement(DATE_NONE, 3068, 3079, DATE_NONE, DATE_NONE)
        .setClanApproximate(false, false, true, false, false)
        .setPrototypeFactions(F_CJF)
        .setProductionFactions(F_CJF)
        .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
