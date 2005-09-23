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
 * 
 */
public class ISBADavidGaussRifle extends BAWeapon {
    /**
     * 
     */
    public ISBADavidGaussRifle() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "David Light Gauss Rifle";
        this.setInternalName(this.name);
        this.addLookupName("BA-ISDavidLightGauss");
        this.rackSize = 1;
        this.shortRange = 3;
        this.mediumRange = 5;
        this.longRange = 8;
        this.extremeRange = 10;
        this.flags |= F_DIRECT_FIRE | F_NO_FIRES | F_BALLISTIC;
    }
}
