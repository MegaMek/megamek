/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.unofficial;

import megamek.common.alphaStrike.AlphaStrikeElement;

/**
 * @author Sebastian Brocks
 * @since Sep 13, 2004
 */
public class CLPlasmaRifle extends PlasmaMFUKWeapon {
    private static final long serialVersionUID = 1758452784566087479L;

    public CLPlasmaRifle() {
        super();
        name = "Plasma Rifle";
        setInternalName("MFUK Plasma Rifle");
        addLookupName("Clan Plasma Rifle");
        addLookupName("CL Plasma Rifle");
        addLookupName("CLPlasmaRifle");
        addLookupName("MFUKCLPlasmaRifle");
        heat = 15;
        damage = 10;
        rackSize = 1;
        minimumRange = 2;
        shortRange = 6;
        mediumRange = 14;
        longRange = 22;
        extremeRange = 28;
        tonnage = 6.0;
        criticals = 2;
        bv = 400;
        cost = 300000;
        // Gonna use the same tech info as the Cannon
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(3068, 3069, 3070, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
    }

    @Override
    public int getBattleForceHeatDamage(int range) {
        if (range <= AlphaStrikeElement.MEDIUM_RANGE) {
            return 3;
        }
        return 0;
    }

    @Override
    public int getAlphaStrikeHeatDamage(int rangeband) {
        if (rangeband <= AlphaStrikeElement.RANGE_BAND_MEDIUM) {
            return 3;
        } else {
            return 0;
        }
    }
}
