/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.missiles;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Mounted;

/**
 * @author Sebastian Brocks
 */
public class CLATM12 extends ATMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -7902048944230263372L;

    /**
     *
     */
    public CLATM12() {
        super();
        name = "ATM 12";
        setInternalName("CLATM12");
        addLookupName("Clan ATM-12");
        heat = 8;
        rackSize = 12;
        minimumRange = 4;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 7.0;
        criticals = 5;
        bv = 212;
        cost = 350000;
        shortAV = 20;
        medAV = 20;
        maxRange = RANGE_MED;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
            .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
            .setClanApproximate(true, true, true, false, false)
            .setPrototypeFactions(F_CCY)
            .setProductionFactions(F_CCY);
    }
    
    @Override
    public double getBattleForceDamage(int range, Mounted linked) {
        if (range <= AlphaStrikeElement.SHORT_RANGE) {
            return 3;
        } else if (range <= AlphaStrikeElement.MEDIUM_RANGE) {
            return 2;
        } else {
            return 1;
        }
    }
}
