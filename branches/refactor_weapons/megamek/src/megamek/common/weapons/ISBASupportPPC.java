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
public class ISBASupportPPC extends BAEnergyWeapon {
    /**
     * 
     */
    public ISBASupportPPC() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_2;
        this.name = "Support PPC";
        this.setInternalName(this.name);
        this.addLookupName("BA-Support PPC");
        this.damage = DAMAGE_VARIABLE;
        this.rackSize = 2;
        this.shortRange = 2;
        this.mediumRange = 5;
        this.longRange = 7;
        this.extremeRange = 10;
        this.flags |= F_PPC| F_DIRECT_FIRE;
    }
}
