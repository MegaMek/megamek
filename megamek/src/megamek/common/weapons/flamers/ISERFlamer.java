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
package megamek.common.weapons.flamers;

import megamek.common.SimpleTechLevel;
import megamek.common.WeaponType;
import megamek.common.alphaStrike.AlphaStrikeElement;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ISERFlamer extends FlamerWeapon {
    private static final long serialVersionUID = 1414639280093120062L;

    public ISERFlamer() {
        super();
        name = "ER Flamer";
        setInternalName(name);
        addLookupName("IS ER Flamer");
        addLookupName("ISERFlamer");
        sortingName = "Flamer X ER";
        flags = flags.or(WeaponType.F_ER_FLAMER);
        heat = 4;
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_2D6;
        shortRange = 3;
        mediumRange = 5;
        longRange = 7;
        extremeRange = 10;
        tonnage = 1;
        criticals = 1;
        bv = 16;
        cost = 15000;
        shortAV = 2;
        maxRange = RANGE_SHORT;
        rulesRefs = "312, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3070, 3081, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setProductionFactions(F_FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public int getAlphaStrikeHeatDamage(int rangeband) {
        if (rangeband <= AlphaStrikeElement.RANGE_BAND_MEDIUM) {
            return 2;
        } else {
            return 0;
        }
    }
}
