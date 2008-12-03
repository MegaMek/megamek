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
public class ISAC2 extends ACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 49211848611799265L;

    /**
     * 
     */
    public ISAC2() {
        super();
        this.techLevel = TechConstants.T_INTRO_BOXSET;
        this.name = "Autocannon/2";
        this.setInternalName(this.name);
        this.addLookupName("IS Auto Cannon/2");
        this.addLookupName("ISAC2");
        this.addLookupName("IS Autocannon/2");
        this.heat = 1;
        this.damage = 2;
        this.rackSize = 2;
        this.minimumRange = 4;
        this.shortRange = 8;
        this.mediumRange = 16;
        this.longRange = 24;
        this.extremeRange = 32;
        this.tonnage = 6.0f;
        this.criticals = 1;
        this.bv = 37;
        this.cost = 75000;
        this.explosive = true; // when firing incendiary ammo
        this.shortAV = 2;
        this.medAV = 2;
        this.longAV = 2;
        this.extAV = 2;
        this.maxRange = RANGE_LONG;
        this.explosionDamage = damage;
    }
}
