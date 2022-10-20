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
package megamek.common.weapons.prototypes;

import megamek.common.Mounted;
import megamek.common.alphaStrike.AlphaStrikeElement;
/**
 * @author Sebastian Brocks
 */
public class CLStreakSRM6Prototype extends CLPrototypeStreakSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2234544642223178737L;

    /**
     *
     */
    public CLStreakSRM6Prototype() {
        super();
        name = "Prototype Streak SRM 6";
        setInternalName("CLStreakSRM6Prototype");
        shortName = "Streak SRM 6 (P)";
        heat = 4;
        rackSize = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 4.5;
        criticals = 2;
        bv = 59;
        cost = 120000;
        shortAV = 8;
        maxRange = RANGE_SHORT;
        rulesRefs = "97, IO";
        flags = flags.or(F_PROTOTYPE).andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_CLAN)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
            .setClanAdvancement(2819, DATE_NONE, DATE_NONE, 2826, DATE_NONE)
            .setClanApproximate(true, false, false, true, false)
            .setPrototypeFactions(F_CSA);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 1.26 :0;
    }
}
