package megamek.common.weapons.lasers;

import megamek.common.SimpleTechLevel;

/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 12, 2004
 *
 */

/**
 * @author Andrew Hunter
 */
public class ISRISCHyperLaser extends LaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 4467522144065588079L;

    /**
     *
     */
    public ISRISCHyperLaser() {
        super();
        name = "RISC Hyper Laser";
        //FIXME - Needs full rules implemented
        setInternalName("ISRISCHyperLaser");
        heat = 24;
        damage = 20;
        toHitModifier = 0;
        shortRange = 8;
        mediumRange = 15;
        longRange = 25;
        extremeRange = 30;
        waterShortRange = 5;
        waterMediumRange = 10;
        waterLongRange = 18;
        waterExtremeRange = 20;
        tonnage = 8.0f;
        criticals = 6;
        bv = 596;
        cost = 750000;
        shortAV = 20;
        medAV = 20;
        maxRange = RANGE_EXT;
        explosionDamage = 10;
        explosive = true;
        rulesRefs = "93,IO";
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
            .setISAdvancement(3134, DATE_NONE, DATE_NONE, 3141, DATE_NONE)
            .setPrototypeFactions(F_RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

    }
}


/*The part this is missing is - any unmodified attack roll result of 2 or 3 by a hyper
laser will result in the same effects as a critical hit against the
weapon, and will deliver no damage to the target (even if the roll
would ordinarily result in a hit).*/