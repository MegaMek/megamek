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
public class ISBASmallLaser extends BAEnergyWeapon {
    /**
     * 
     */
    public ISBASmallLaser() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Small Laser";
        this.setInternalName("BASmallLaser");
        this.addLookupName("BA-Small Laser");
        this.damage = DAMAGE_VARIABLE;
        this.rackSize = 3;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.flags |= F_DIRECT_FIRE | F_LASER | F_NO_FIRES;
    }
}
