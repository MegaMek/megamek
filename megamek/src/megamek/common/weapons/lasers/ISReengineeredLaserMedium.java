/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.lasers;

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 * @since Sep 2, 2004
 */
public class ISReengineeredLaserMedium extends ReengineeredLaserWeapon {
    private static final long serialVersionUID = 1596494785198942212L;

    public ISReengineeredLaserMedium() {
        super();
        name = "Medium Re-engineered Laser";
        setInternalName(name);
        addLookupName("ISMediumReengineeredLaser");
        addLookupName("ISMediumRELaser");
        sortingName = "Laser REENG C";
        toHitModifier = -1;
        heat = 6;
        damage = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        waterShortRange = 2;
        waterMediumRange = 4;
        waterLongRange = 6;
        waterExtremeRange = 8;
        tonnage = 2.5;
        criticals = 2;
        bv = 65;
        cost = 100000;
        shortAV = 6;
        maxRange = RANGE_SHORT;
        rulesRefs = "89, IO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_D)
                .setISAdvancement(3120, 3130, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
