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
package megamek.common.weapons.lasers;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class ISERLaserSmall extends LaserWeapon {
    private static final long serialVersionUID = -4997798107691083605L;

    public ISERLaserSmall() {
        super();
        name = "ER Small Laser";
        setInternalName("ISERSmallLaser");
        addLookupName("IS ER Small Laser");
        sortingName = "Laser ER B";
        heat = 2;
        damage = 3;
        shortRange = 2;
        mediumRange = 4;
        longRange = 5;
        extremeRange = 8;
        waterShortRange = 1;
        waterMediumRange = 2;
        waterLongRange = 3;
        waterExtremeRange = 4;
        tonnage = 0.5;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 17;
        cost = 11250;
        shortAV = 3;
        maxRange = RANGE_SHORT;
        rulesRefs = "226, TM";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        // December 2021 - Errata request to change common date
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_C)
                .setISAdvancement(3052, 3058, 3062, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_FW, F_WB)
                .setProductionFactions(F_FW);
    }

    @Override
    public boolean isAlphaStrikePointDefense() {
        return true;
    }
}
