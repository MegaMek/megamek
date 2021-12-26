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

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class PrototypeRL15 extends PrototypeRLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5888570332510350564L;

    /**
     *
     */
    public PrototypeRL15() {
        super();

        name = "Prototype Rocket Launcher 15";
        setInternalName("CLRocketLauncher15Prototype");
        addLookupName("ISPrimRL15");
        shortName = "RL/15 (P)";
        heat = 4;
        rackSize = 15;
        shortRange = 4;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        tonnage = 1.0;
        criticals = 2;
        bv = 21;
        cost = 30000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        flags = flags.or(F_PROTOTYPE);
        rulesRefs = "217,IO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
            .setIntroLevel(false)
            .setUnofficial(false)
            .setTechRating(RATING_B)
            .setAvailability(RATING_D, RATING_F, RATING_X, RATING_X)
            .setISAdvancement(DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(true, false, false,false, false)
            .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
            .setClanApproximate(true, false, false, true, false)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
