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
public class ISHVAC10 extends HVACWeapon {
    private static final long serialVersionUID = 4958849713169213573L;

    public ISHVAC10() {
        super();
        name = "HVAC/10";
        setInternalName("Hyper Velocity Auto Cannon/10");
        addLookupName("IS Hyper Velocity Auto Cannon/10");
        addLookupName("ISHVAC10");
        addLookupName("IS Hyper Velocity Autocannon/10");
        heat = 7;
        damage = 10;
        rackSize = 10;
        shortRange = 6;
        mediumRange = 12;
        longRange = 20;
        extremeRange = 24;
        tonnage = 14.0;
        criticals = 6;
        bv = 158;
        cost = 230000;
        shortAV = 10;
        medAV = 10;
        longAV = 10;
        maxRange = RANGE_LONG;
        explosionDamage = 10;
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
