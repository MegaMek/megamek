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
public class CLATM3 extends ATMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 107949833660086492L;

    /**
     *
     */
    public CLATM3() {
        super();
        name = "ATM 3";
        setInternalName("CLATM3");
        addLookupName("Clan ATM-3");
        heat = 2;
        rackSize = 3;
        minimumRange = 4;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 1.5;
        criticals = 2;
        bv = 53;
        cost = 50000;
        shortAV = 4;
        medAV = 4;
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
            return 0.6;
        } else if (range <= AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.4;
        } else {
            return 0.2;
        }
    }
    
}
