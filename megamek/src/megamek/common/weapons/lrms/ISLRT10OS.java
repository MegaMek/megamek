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
public class ISLRT10OS extends LRTWeapon {
    private static final long serialVersionUID = 2674309948460871883L;

    public ISLRT10OS() {
        super();

        name = "LRT 10 (OS)";
        setInternalName(name);
        addLookupName("IS OS LRT-10");
        addLookupName("ISLRTorpedo10 (OS)");
        addLookupName("IS LRT 10 (OS)");
        addLookupName("ISLRT10OS");
        heat = 4;
        rackSize = 10;
        minimumRange = 6;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 5.5;
        criticals = 2;
        bv = 18;
        flags = flags.or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 50000;
        rulesRefs = "230, TM";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2665, 2676, 3045, 2800, 3030)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_FW);
    }
}
