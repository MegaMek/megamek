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
 * @author beerockxs
 */
public class ISFluidGun extends FluidGunWeapon{

    /**
     *
     */
    private static final long serialVersionUID = 5043640099544278749L;

    /**
     *
     */
    public ISFluidGun() {
        super();
        techLevel = TechConstants.T_IS_ADVANCED;
        name = "Fluid Gun";
        setInternalName("IS Fluid Gun");
        addLookupName("ISFluidGun");
        rackSize = 1;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        bv = 6;
        heat = 0;
        criticals = 2;
        tonnage = 2;
        cost = 35000;
    }
}
