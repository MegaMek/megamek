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

/**
 * @author Sebastian Brocks
 */
public class CLATM9 extends ATMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3779719958622540629L;

    /**
     *
     */
    public CLATM9() {
        super();
        name = "ATM 9";
        setInternalName("CLATM9");
        addLookupName("Clan ATM-9");
        heat = 6;
        rackSize = 9;
        minimumRange = 4;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 5.0;
        criticals = 4;
        bv = 147;
        cost = 225000;
        shortAV = 14;
        medAV = 14;
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
}
