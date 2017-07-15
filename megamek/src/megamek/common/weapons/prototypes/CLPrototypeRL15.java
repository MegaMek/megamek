/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.weapons.prototypes;

import megamek.common.TechAdvancement;
import megamek.common.weapons.PrototypeRLWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLPrototypeRL15 extends PrototypeRLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5888570332510350564L;

    /**
     *
     */
    public CLPrototypeRL15() {
        super();

        name = "Rocket Launcher 15 (PP)";
        setInternalName("CLRocketLauncher15Prototype");
        heat = 5;
        rackSize = 15;
        shortRange = 4;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        tonnage = 1.0f;
        criticals = 2;
        bv = 21;
        cost = 30000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        rulesRefs = "229,TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(2807, DATE_NONE, DATE_NONE, 2823);
        techAdvancement.setTechRating(RATING_B);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_B, RATING_X, RATING_X });
    }
}

//These have been merged with the IS RL's per IO