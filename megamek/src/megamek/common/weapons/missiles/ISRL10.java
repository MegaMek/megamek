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
package megamek.common.weapons.missiles;

import megamek.common.TechAdvancement;
import megamek.common.weapons.RLWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISRL10 extends RLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3437644808445570760L;

    /**
     *
     */
    public ISRL10() {
        super();

        name = "Rocket Launcher 10";
        setInternalName("RL10");
        addLookupName("RL 10");
        addLookupName("ISRocketLauncher10");
        addLookupName("IS RLauncher-10");
        heat = 3;
        rackSize = 10;
        shortRange = 5;
        mediumRange = 11;
        longRange = 18;
        extremeRange = 22;
        tonnage = .5f;
        criticals = 1;
        bv = 18;
        cost = 15000;
        shortAV = 6;
        medAV = 6;
        maxRange = RANGE_MED;
        rulesRefs = "229,TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_B)
            .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
            .setISAdvancement(DATE_ES, 3064, 3067, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
            .setClanApproximate(false, false, false,false, false)
            .setProductionFactions(F_MH);
    }
}
