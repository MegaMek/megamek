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
package megamek.common.weapons.srms;

import megamek.common.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;

import static megamek.common.MountedHelper.*;
/**
 * @author Sebastian Brocks
 */
public class CLSRM4 extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6776541552712952370L;

    /**
     *
     */
    public CLSRM4() {
        super();
        name = "SRM 4";
        setInternalName("CLSRM4");
        addLookupName("Clan SRM-4");
        addLookupName("Clan SRM 4");
        heat = 3;
        rackSize = 4;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 1.0;
        criticals = 1;
        bv = 39;
        flags = flags.or(F_NO_FIRES);
        cost = 60000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
            .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
            .setClanApproximate(true, false, false,false, false)
            .setPrototypeFactions(F_CCC)
            .setProductionFactions(F_CCC);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (isArtemisV(fcs)) {
            return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.63 : 0;
        } else {
            return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.6 : 0;
        }
    }
}
