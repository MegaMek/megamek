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

/**
 * @author Sebastian Brocks
 */
public class ISStreakSRM2 extends StreakSRMWeapon {
    private static final long serialVersionUID = 2636425754066916235L;

    public ISStreakSRM2() {
        super();

        name = "Streak SRM 2";
        setInternalName("ISStreakSRM2");
        addLookupName("IS Streak SRM-2");
        addLookupName("IS Streak SRM 2");
        heat = 2;
        rackSize = 2;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 1.5;
        criticals = 1;
        flags = flags.or(F_NO_FIRES);
        bv = 30;
        cost = 15000;
        shortAV = 4;
        maxRange = RANGE_SHORT;
        rulesRefs = "230, TM";
        flags = flags.andNot(F_PROTO_WEAPON);
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_D)
                .setISAdvancement(2645, 2647, 2650, 2845, 3035)
                .setISApproximate(false, false, true,false, false)
                .setClanAdvancement(2645, 2647, 2650, 2845, DATE_NONE)
                .setClanApproximate(false, false, true,false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
    }
}
