/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
/*
 * Created on Sep 13, 2004
 *
 */
package megamek.common.weapons.ppc;

import megamek.common.BattleForceElement;
import megamek.common.TechAdvancement;
import megamek.common.weapons.PlasmaMFUKWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLPlasmaRifle extends PlasmaMFUKWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 1758452784566087479L;

    /**
     *
     */
    public CLPlasmaRifle() {
        super();
        name = "Plasma Rifle";
        setInternalName("MFUK Plasma Rifle");
        addLookupName("Clan Plasma Rifle");
        addLookupName("CL Plasma Rifle");
        addLookupName("CLPlasmaRifle");
        addLookupName("MFUKCLPlasmaRifle");
        heat = 15;
        damage = 10;
        rackSize = 1;
        minimumRange = 2;
        shortRange = 6;
        mediumRange = 14;
        longRange = 22;
        extremeRange = 28;
        waterShortRange = 4;
        waterMediumRange = 10;
        waterLongRange = 15;
        waterExtremeRange = 20;
        tonnage = 6.0f;
        criticals = 2;
        bv = 400;
        cost = 300000;
        //Gonna use the same tech info as the Cannon
        
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_ALL);
        techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, 3069);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_E, RATING_X });
    }

    @Override
    public int getBattleForceHeatDamage(int range) {
        if (range <= BattleForceElement.MEDIUM_RANGE) {
            return 3;
        }
        return 0;
    }
}
