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
package megamek.common.weapons.missiles;

/**
 * @author Sebastian Brocks
 */
public class RocketLauncher15 extends RLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8464817815813827947L;

    /**
     *
     */
    public RocketLauncher15() {
        super();

        name = "Rocket Launcher 15";
        setInternalName("RL15");
        addLookupName("ISRocketLauncher15");
        addLookupName("RL 15");
        addLookupName("IS RLauncher-15");
        addLookupName("Rocket Launcher 15");
        heat = 4;
        rackSize = 15;
        shortRange = 4;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        tonnage = 1.0;
        criticals = 2;
        bv = 23;
        cost = 30000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        rulesRefs = "229, TM";
        techAdvancement.setTechBase(TECH_BASE_ALL)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_B)
            .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
            .setISAdvancement(DATE_NONE, 3064, 3067, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setClanAdvancement(DATE_NONE, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
            .setClanApproximate(false, false, false,false, false)
            .setProductionFactions(F_MH);
    }
}
