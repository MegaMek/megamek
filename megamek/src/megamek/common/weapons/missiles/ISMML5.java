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

import static megamek.common.MountedHelper.*;

/**
 * @author Sebastian Brocks
 */
public class ISMML5 extends MMLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -546200914895806968L;

    /**
     *
     */
    public ISMML5() {
        super();
        name = "MML 5";
        setInternalName("ISMML5");
        addLookupName("IS MML-5");
        heat = 3;
        rackSize = 5;
        tonnage = 3;
        criticals = 3;
        bv = 45;
        cost = 75000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        maxRange = RANGE_LONG;
        rulesRefs = "229, TM";
        //March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_D)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, true,false, false)
            .setProductionFactions(F_MERC,F_WB);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return (isArtemisIV(fcs) || isArtemisProto(fcs)) ? 0.8 : 0.6;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return (isArtemisIV(fcs) || isArtemisProto(fcs)) ? 0.6 : 0.45;
        } else if (range == AlphaStrikeElement.LONG_RANGE) {
            return (isArtemisIV(fcs) || isArtemisProto(fcs)) ? 0.4 : 0.3;
        } else {
            return 0;
        }
    }
}
