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

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Mounted;

/**
 * @author Andrew Hunter
 * @since Oct 2, 2004
 */
public class CLUAC2 extends UACWeapon {
    private static final long serialVersionUID = 7982946203794957045L;

    public CLUAC2() {
        super();
        name = "Ultra AC/2";
        setInternalName("CLUltraAC2");
        addLookupName("Clan Ultra AC/2");
        sortingName = "Ultra AC/02";
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 2;
        shortRange = 9;
        mediumRange = 18;
        longRange = 27;
        extremeRange = 36;
        tonnage = 5.0;
        criticals = 2;
        bv = 62;
        cost = 120000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        extAV = 3;
        maxRange = RANGE_EXT;
        explosionDamage = damage;
        rulesRefs = "208, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_D, RATING_C)
                .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false)
                .setPrototypeFactions(F_CLAN)
                .setProductionFactions(F_CLAN);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted ignore) {
        return range == AlphaStrikeElement.SHORT_RANGE ? 0.249 : 0.3;
    }
}
