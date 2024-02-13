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
import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class ISMML3 extends MMLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -9170270710231973218L;

    /**
     *
     */
    public ISMML3() {
        super();
        name = "MML 3";
        setInternalName("ISMML3");
        addLookupName("IS MML-3");
        heat = 2;
        rackSize = 3;
        tonnage = 1.5;
        criticals = 2;
        bv = 29;
        cost = 45000;
        shortAV = 2;
        medAV = 2;
        longAV = 2;
        maxRange = RANGE_LONG;
        rulesRefs = "229, TM";
        //March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_D)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false, false, false)
            .setProductionFactions(F_MERC,F_WB)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.4;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.3;
        } else if (range == AlphaStrikeElement.LONG_RANGE) {
            return 0.2;
        } else {
            return 0;
        }
    }
}
