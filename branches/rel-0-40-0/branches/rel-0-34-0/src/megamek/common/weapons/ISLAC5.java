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
/*
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

public class ISLAC5 extends LACWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 6131945194809316957L;

    public ISLAC5() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Light Auto Cannon/5";
        this.setInternalName(this.name);
        this.addLookupName("IS Light Auto Cannon/5");
        this.addLookupName("ISLAC5");
        this.addLookupName("IS Light Autocannon/5");
        this.heat = 1;
        this.damage = 5;
        this.rackSize = 5;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 5.0f;
        this.criticals = 2;
        this.bv = 62;
        this.cost = 150000;
        this.explosionDamage = damage;
    }
}
