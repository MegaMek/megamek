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
package megamek.common.weapons.autocannons;

import megamek.common.SimpleTechLevel;

/**
 * @author Jason Tighe
 * @since Sep 25, 2004
 */
public class ISHVAC2 extends HVACWeapon {
    private static final long serialVersionUID = 4958849713169213573L;

    public ISHVAC2() {
        super();
        name = "HVAC/2";
        setInternalName("Hyper Velocity Auto Cannon/2");
        addLookupName("IS Hyper Velocity Auto Cannon/2");
        addLookupName("ISHVAC2");
        addLookupName("IS Hyper Velocity Autocannon/2");
        sortingName = "HVAC/02";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 3;
        shortRange = 10;
        mediumRange = 20;
        longRange = 35;
        extremeRange = 40;
        tonnage = 8.0;
        criticals = 2;
        bv = 53;
        cost = 100000;
        shortAV = 2;
        medAV = 2;
        longAV = 2;
        extAV = 2;
        maxRange = RANGE_EXT;
        explosionDamage = 2;
        rulesRefs = "285,TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3059, 3079)
                .setPrototypeFactions(F_CC).setProductionFactions(F_CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
}
