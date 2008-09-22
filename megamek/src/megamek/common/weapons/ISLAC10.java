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

public class ISLAC10 extends LACWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 7715730019995031625L;

    public ISLAC10() {
        super();
        this.techLevel = TechConstants.T_IS_UNOFFICIAL;
        this.name = "Light Auto Cannon/10";
        this.setInternalName(this.name);
        this.addLookupName("IS Light AutoCannon/10");
        this.addLookupName("ISLAC10");
        this.addLookupName("IS Light Autocannon/10");
        this.heat = 3;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.extremeRange = 12;
        this.tonnage = 8.0f;
        this.criticals = 4;
        this.bv = 74;
        this.cost = 225000;
        this.explosionDamage = damage;
    }
}
