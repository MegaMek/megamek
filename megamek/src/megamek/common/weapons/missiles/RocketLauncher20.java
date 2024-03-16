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
public class RocketLauncher20 extends RLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -1220608344459915265L;

    /**
     *
     */
    public RocketLauncher20() {
        super();

        name = "Rocket Launcher 20";
        setInternalName("RL20");
        addLookupName("ISRocketLauncher20");
        addLookupName("RL 20");
        addLookupName("IS RLauncher-20");
        addLookupName("Rocket Launcher 20");
        heat = 5;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 7;
        longRange = 12;
        extremeRange = 14;
        tonnage = 1.5;
        criticals = 3;
        bv = 24;
        cost = 45000;
        shortAV = 12;
        medAV = 12;
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
