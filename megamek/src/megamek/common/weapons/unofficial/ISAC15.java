/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.unofficial;

import megamek.common.weapons.autocannons.ACWeapon;

/**
 * @author BATTLEMASTER IIC
 * @since Sep 25, 2004
 */
public class ISAC15 extends ACWeapon {
    private static final long serialVersionUID = 814114264108820161L;

    public ISAC15() {
        super();
        name = "AC/15";
        setInternalName("Autocannon/15");
        addLookupName("IS Auto Cannon/15");
        addLookupName("Auto Cannon/15");
        addLookupName("AutoCannon/15");
        addLookupName("AC/15");
        addLookupName("ISAC15");
        addLookupName("IS Autocannon/15");
        heat = 5;
        damage = 15;
        rackSize = 15;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        tonnage = 13.0;
        criticals = 8;
        bv = 178;
        cost = 250000;
        shortAV = 15;
        medAV = 15;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        // This being an official Weapon I'm using the AC20 information
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setUnofficial(true)
                .setTechRating(RATING_C)
                .setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(2488, 2500, 2502, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2488, 2500, 2502, 2850, DATE_NONE)
                .setClanApproximate(false, false, false, true, false)
                .setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
    }
}
