/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.weapons.autocannons;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Mounted;
/**
 * @author Andrew Hunter
 * @since Oct 15, 2004
 */
public class CLLB5XAC extends LBXACWeapon {
    private static final long serialVersionUID = 722040764690180243L;

    public CLLB5XAC() {
        super();
        name = "LB 5-X AC";
        setInternalName("CLLBXAC5");
        addLookupName("Clan LB 5-X AC");
        sortingName = "LB 05-X AC";
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 3;
        shortRange = 8;
        mediumRange = 15;
        longRange = 24;
        extremeRange = 30;
        tonnage = 7.0;
        criticals = 4;
        bv = 93;
        cost = 250000;
        shortAV = 5;
        medAV = 5;
        longAV = 5;
        maxRange = RANGE_LONG;
        rulesRefs = "207, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false)
                .setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.236;
        } else if (range <= AlphaStrikeElement.LONG_RANGE) {
            return 0.315;
        } else {
            return 0;
        }
    }
}
