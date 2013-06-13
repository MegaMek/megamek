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
public class CLPrototypeRL20 extends PrototypeRLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -1726964787852929811L;

    /**
     *
     */
    public CLPrototypeRL20() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Rocket Launcher 20 (PP)";
        setInternalName("CLRocketLauncher20Prototype");
        heat = 5;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 7;
        longRange = 12;
        extremeRange = 14;
        tonnage = 1.5f;
        criticals = 3;
        bv = 22;
        cost = 45000;
        shortAV = 12;
        medAV = 12;
        maxRange = RANGE_MED;
        introDate = 2801;
        techLevel.put(2801, techLevel.get(3071));
    }
}
