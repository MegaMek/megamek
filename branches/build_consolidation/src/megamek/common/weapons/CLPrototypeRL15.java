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

import megamek.common.TechConstants;

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
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
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
        introDate = 2801;
        techLevel.put(2801, techLevel.get(3071));
    }
}
