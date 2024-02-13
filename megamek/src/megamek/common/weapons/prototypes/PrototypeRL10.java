/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.prototypes;

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class PrototypeRL10 extends PrototypeRLWeapon {
    private static final long serialVersionUID = -8226462713763738211L;

    public PrototypeRL10() {
        super();

        name = "Prototype Rocket Launcher 10";
        setInternalName("CLRocketLauncher10Prototype");
        shortName = "RL/10 (P)";
        heat = 3;
        rackSize = 10;
        shortRange = 5;
        mediumRange = 11;
        longRange = 18;
        extremeRange = 22;
        tonnage = .5;
        criticals = 1;
        bv = 15;
        cost = 15000;
        shortAV = 6;
        medAV = 6;
        flags = flags.or(F_PROTOTYPE);
        maxRange = RANGE_MED;
        rulesRefs = "217, IO";
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_B)
                .setAvailability(RATING_D, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
                .setClanApproximate(true, false, false, true, false)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}

