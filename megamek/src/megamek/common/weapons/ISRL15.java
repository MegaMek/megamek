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
package megamek.common.weapons;

import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class ISRL15 extends RLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8464817815813827947L;

    /**
     *
     */
    public ISRL15() {
        super();

        name = "Rocket Launcher 15";
        setInternalName("RL15");
        addLookupName("ISRocketLauncher15");
        addLookupName("RL 15");
        addLookupName("IS RLauncher-15");
        heat = 4;
        rackSize = 15;
        shortRange = 4;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        tonnage = 1.0f;
        criticals = 2;
        bv = 23;
        cost = 30000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2200, 3064, 3067);
        techAdvancement.setTechRating(RATING_B);
        techAdvancement.setAvailability( new int[] { RATING_B, RATING_B, RATING_B, RATING_B });
    }
}
