/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * @author Andrew Hunter
 */
public class ISAC5 extends ACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 8756042527483383101L;

    /**
     * 
     */
    public ISAC5() {
        super();
        this.techLevel = TechConstants.T_IS_LEVEL_1;
        this.name = "Auto Cannon/5";
        this.setInternalName(this.name);
        this.addLookupName("IS Auto Cannon/5");
        this.addLookupName("ISAC5");
        this.addLookupName("IS Autocannon/5");
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.minimumRange = 3;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 8.0f;
        this.criticals = 4;
        this.bv = 70;
        this.cost = 125000;
        this.shortAV = 5;
        this.medAV = 5;
        this.maxRange = RANGE_MED;
        this.explosionDamage = damage;
    }
}
