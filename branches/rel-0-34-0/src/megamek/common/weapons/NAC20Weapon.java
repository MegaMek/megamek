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
public class NAC20Weapon extends NavalACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public NAC20Weapon() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Naval AC 20";
        this.setInternalName(this.name);
        this.addLookupName("NAC20");
        this.heat = 60;
        this.damage = 20;
        this.rackSize = 20;
        this.shortRange = 11;
        this.mediumRange = 21;
        this.longRange = 31;
        this.extremeRange = 42;
        this.tonnage = 2500.0f;
        this.bv = 3792;
        this.cost = 5000000;
        this.shortAV = 20;
        this.medAV = 20;
        this.longAV = 20;
        this.maxRange = RANGE_LONG;
    
    }
}
