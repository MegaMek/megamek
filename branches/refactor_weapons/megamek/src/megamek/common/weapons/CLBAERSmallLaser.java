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
public class CLBAERSmallLaser extends BAEnergyWeapon {
    /**
     * 
     */
    public CLBAERSmallLaser() {
        super();
        this.techLevel = TechConstants.T_CLAN_LEVEL_2;
        this.name = "ER Small Laser";
        this.setInternalName("BACLERSmallLaser");
        this.addLookupName("BA-Clan ER Small Laser");
        this.damage = DAMAGE_VARIABLE;
        this.rackSize = 5;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE | F_NO_FIRES;
    }
}
