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
public class NAC10Weapon extends NavalACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public NAC10Weapon() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Naval AC 10";
        this.setInternalName(this.name);
        this.addLookupName("NAC10");
        this.heat = 30;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 11;
        this.mediumRange = 22;
        this.longRange = 33;
        this.extremeRange = 44;
        this.tonnage = 2000.0f;
        this.bv = 1896;
        this.cost = 2000000;
        this.shortAV = 10;
        this.medAV = 10;
        this.longAV = 10;
        this.maxRange = RANGE_LONG;
    }
}
