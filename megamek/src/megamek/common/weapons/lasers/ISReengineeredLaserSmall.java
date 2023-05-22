/*
 * MegaMek -
 * Copyright (C) 2013 Ben Mazur (bmazur@sev.org)
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

public class ISReengineeredLaserSmall extends ReengineeredLaserWeapon {
    private static final long serialVersionUID = 6231212510603930740L;

    public ISReengineeredLaserSmall() {
        super();
        name = "Small Re-engineered Laser";
        setInternalName(name);
        addLookupName("ISSmallReengineeredLaser");
        addLookupName("ISSmallRELaser");
        sortingName = "Laser REENG B";
        toHitModifier = -1;
        heat = 4;
        damage = 4;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 2;
        waterExtremeRange = 4;
        tonnage = 1.5;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 14;
        cost = 25000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        atClass = CLASS_POINT_DEFENSE;
        rulesRefs = "89, IO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_D)
                .setISAdvancement(3120, 3130,DATE_NONE,DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
    
    @Override
    public boolean isAlphaStrikePointDefense() {
        return true;
    }
}
