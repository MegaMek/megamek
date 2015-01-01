/* MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Jay Lawson
 */
public class NAC35Weapon extends NavalACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public NAC35Weapon() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Naval AC 35";
        this.setInternalName(this.name);
        this.addLookupName("NAC35");
        this.heat = 120;
        this.damage = 35;
        this.rackSize = 35;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 21;
        this.extremeRange = 28;
        this.tonnage = 4000.0f;
        this.bv = 4956;
        this.cost = 14000000;       
        this.shortAV = 35;
        this.medAV = 35;
        this.maxRange = RANGE_MED;
    
    }
}
