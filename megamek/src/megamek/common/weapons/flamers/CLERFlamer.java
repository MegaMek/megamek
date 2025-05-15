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
public class CLERFlamer extends FlamerWeapon {
    private static final long serialVersionUID = 1414639280093120062L;

    public CLERFlamer() {
        super();
        name = "ER Flamer";
        setInternalName("CLERFlamer");
        addLookupName("CL ER Flamer");
        sortingName = "Flamer ER";
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
        techAdvancement.setTechBase(TechBase.CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.D)
                .setAvailability(TechRating.X, TechRating.X, TechRating.E, TechRating.D)
                .setClanAdvancement(DATE_NONE, 3067, 3081, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(Faction.CJF)
                .setProductionFactions(Faction.CJF)
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
