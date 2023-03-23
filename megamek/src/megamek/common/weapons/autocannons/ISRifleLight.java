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
public class ISRifleLight extends RifleWeapon {
    private static final long serialVersionUID = -8493561115043967893L;

    public ISRifleLight() {
        super();
        name = "Rifle (Cannon, Light)";
        setInternalName(name);
        shortName = "Light Rifle";
        addLookupName("IS Light Rifle");
        addLookupName("ISLightRifle");
        sortingName = "Rifle Cannon B";
        heat = 1;
        damage = 3;
        rackSize = 3;
        minimumRange = 0;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 18;
        tonnage = 3.0;
        criticals = 1;
        bv = 21;
        cost = 37750;
        explosive = false; // when firing incendiary ammo
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        extAV = 3;
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
