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

public class ISLAC2 extends LACWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 3128546525878614842L;

    public ISLAC2() {
        super();
        this.techLevel = TechConstants.T_IS_TW_NON_BOX;
        this.name = "Light Auto Cannon/2";
        this.setInternalName(this.name);
        this.addLookupName("IS Light AutoCannon/2");
        this.addLookupName("ISLAC2");
        this.addLookupName("IS Light Autocannon/2");
        this.heat = 1;
        this.damage = 2;
        this.rackSize = 2;
        this.shortRange = 6;
        this.mediumRange = 12;
        this.longRange = 18;
        this.extremeRange = 24;
        this.tonnage = 4.0f;
        this.criticals = 1;
        this.bv = 30;
        this.cost = 100000;
        this.explosionDamage = damage;
    }
}
