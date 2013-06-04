/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLVehicularGrenadeLauncher extends VehicularGrenadeLauncherWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2615923567135586999L;

    /**
     *
     */
    public CLVehicularGrenadeLauncher() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        name = "Vehicular Grenade Launcher";
        setInternalName("CLVehicularGrenadeLauncher");
        techRating = RATING_C;
        availRating = new int[] { RATING_D, RATING_E, RATING_F };
        introDate = 1950;
        techLevel.put(1950, techLevel.get(3071));
        techLevel.put(3080, TechConstants.T_CLAN_TW);
    }

}
