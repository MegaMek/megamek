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
package megamek.common.weapons.autocannons;

import megamek.common.SimpleTechLevel;

/**
 * @author Jason Tighe
 * @since Sep 25, 2004
 */
public class ISRifleHeavy extends RifleWeapon {
    private static final long serialVersionUID = -2670817452732971454L;

    public ISRifleHeavy() {
        super();
        name = "Rifle (Cannon, Heavy)";
        setInternalName(name);
        shortName = "Heavy Rifle";
        addLookupName("IS Heavy Rifle");
        addLookupName("ISHeavyRifle");
        sortingName = "Rifle Cannon D";
        heat = 4;
        damage = 9;
        rackSize = 9;
        minimumRange = 2;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 27;
        tonnage = 8.0;
        criticals = 3;
        bv = 91;
        cost = 90000;
        explosive = false; // when firing incendiary ammo
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        extAV = 9;
        maxRange = RANGE_MED;
        explosionDamage = 0;
        rulesRefs = "338, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_B)
                .setAvailability(RATING_C, RATING_F, RATING_X, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, 3084, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
