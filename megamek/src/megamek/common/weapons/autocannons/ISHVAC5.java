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
public class ISHVAC5 extends HVACWeapon {
    private static final long serialVersionUID = -1116752747486372187L;

    public ISHVAC5() {
        super();
        name = "HVAC/5";
        setInternalName("Hyper Velocity Auto Cannon/5");
        addLookupName("IS Hyper Velocity Auto Cannon/5");
        addLookupName("ISHVAC5");
        addLookupName("IS Hyper Velocity Autocannon/5");
        sortingName = "HVAC/05";
        heat = 3;
        damage = 5;
        rackSize = 5;
        shortRange = 8;
        mediumRange = 16;
        longRange = 28;
        extremeRange = 32;
        tonnage = 12.0;
        criticals = 4;
        bv = 109;
        cost = 160000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        extAV = 5;
        maxRange = RANGE_EXT;
        explosionDamage = 5;
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
