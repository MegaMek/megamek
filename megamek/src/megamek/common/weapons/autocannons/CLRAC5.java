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
package megamek.common.weapons.autocannons;

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 * @since Oct 19, 2004
 */
public class CLRAC5 extends RACWeapon {
    private static final long serialVersionUID = -9054458663836717481L;

    public CLRAC5() {
        super();

        name = "Rotary AC/5";
        setInternalName("CLRotaryAC5");
        addLookupName("Clan Rotary AC/5");
        addLookupName("Clan Rotary Assault Cannon/5");
        heat = 1;
        damage = 5;
        rackSize = 5;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 10.0;
        criticals = 8;
        bv = 345;
        cost = 275000;
        shortAV = 20;
        medAV = 20;
        longAV = 20;
        maxRange = RANGE_LONG;
        rulesRefs = "286, TO";
        flags = flags.andNot(F_PROTO_WEAPON);
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3073, DATE_NONE, 3104, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_CSF).setProductionFactions(F_CSF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
