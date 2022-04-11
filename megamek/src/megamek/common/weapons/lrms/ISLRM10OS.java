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
package megamek.common.weapons.lrms;

/**
 * @author Sebastian Brocks
 */
public class ISLRM10OS extends LRMWeapon {
    private static final long serialVersionUID = -2792101005477263443L;

    public ISLRM10OS() {
        super();
        name = "LRM 10 (OS)";
        setInternalName(name);
        addLookupName("IS OS LRM-10");
        addLookupName("ISLRM10 (OS)");
        addLookupName("IS LRM 10 (OS)");
        heat = 4;
        rackSize = 10;
        minimumRange = 6;
        tonnage = 5.5;
        criticals = 2;
        bv = 18;
        flags = flags.or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 50000;
        shortAV = 6;
        medAV = 6;
        longAV = 6;
        maxRange = RANGE_LONG;
        rulesRefs = "230, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2665, 2676, 3045, 2800, 3030)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2665, 2676, 3045, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_FW);
    }
}
