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
public class NAC30Weapon extends NavalACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public NAC30Weapon() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Naval AC 30";
        this.setInternalName(this.name);
        this.addLookupName("NAC30");
        this.heat = 100;
        this.damage = 30;
        this.rackSize = 30;
        this.shortRange = 9;
        this.mediumRange = 18;
        this.longRange = 27;
        this.extremeRange = 36;
        this.tonnage = 3500.0f;
        this.bv = 5688;
        this.cost = 10500000;
        this.shortAV = 30;
        this.medAV = 30;
        this.longAV = 30;
        this.maxRange = RANGE_LONG;
    
    }
}
