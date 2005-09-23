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
public class ISBATsunamiHeavyGaussRifle extends BAWeapon {
    /**
     * 
     */
    public ISBATsunamiHeavyGaussRifle() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Tsunami Heavy Gauss Rifle";
        this.setInternalName(this.name);
        this.addLookupName("BA-ISTsunamiHeavyGaussRifle");
        this.heat = 0;
        this.rackSize = 1;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 5;
        this.extremeRange = 8;
        this.flags |= F_DIRECT_FIRE | F_NO_FIRES | F_BALLISTIC;
    }
}
