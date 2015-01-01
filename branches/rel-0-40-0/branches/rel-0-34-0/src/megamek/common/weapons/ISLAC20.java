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

public class ISLAC20 extends LACWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 7135078308771443835L;

    public ISLAC20() {
        super();
        this.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        this.name = "Light Auto Cannon/20";
        this.setInternalName(this.name);
        this.addLookupName("IS Light AutoCannon/20");
        this.addLookupName("ISLAC20");
        this.addLookupName("IS Light Autocannon/20");
        this.heat = 7;
        this.damage = 20;
        this.rackSize = 20;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.tonnage = 9.0f;
        this.criticals = 6;
        this.bv = 118;
        this.cost = 325000;
        this.explosionDamage = damage;
    }
}
