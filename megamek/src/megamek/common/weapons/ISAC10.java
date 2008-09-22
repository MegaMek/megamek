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
public class ISAC10 extends ACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 814114264108820161L;

    /**
     * 
     */
    public ISAC10() {
        super();
        this.techLevel = TechConstants.T_INTRO_BOXSET;
        this.name = "Auto Cannon/10";
        this.setInternalName(this.name);
        this.addLookupName("IS Auto Cannon/10");
        this.addLookupName("ISAC10");
        this.addLookupName("IS Autocannon/10");
        this.heat = 3;
        this.damage = 10;
        this.rackSize = 10;
        this.shortRange = 5;
        this.mediumRange = 10;
        this.longRange = 15;
        this.extremeRange = 20;
        this.tonnage = 12.0f;
        this.criticals = 7;
        this.bv = 123;
        this.cost = 200000;
        this.shortAV = 10;
        this.medAV = 10;
        this.maxRange = RANGE_MED;
        this.explosionDamage = damage;
    }
}
