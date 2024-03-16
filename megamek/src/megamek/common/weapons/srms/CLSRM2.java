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
public class CLSRM2 extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8216939998088201265L;

    /**
     *
     */
    public CLSRM2() {
        super();
        name = "SRM 2";
        setInternalName("CLSRM2");
        addLookupName("Clan SRM-2");
        addLookupName("Clan SRM 2");
        heat = 2;
        rackSize = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 0.5;
        criticals = 1;
        bv = 21;
        flags = flags.or(F_NO_FIRES);
        cost = 10000;
        shortAV = 2;
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
        if (isArtemisIV(fcs) || isArtemisProto(fcs)) {
            return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.4 : 0;
        } else if (isArtemisV(fcs)) {
            return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.42 : 0;
        } else {
            return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 0.2 : 0;
        }
    }
}
