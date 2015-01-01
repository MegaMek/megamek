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
        techLevel = TechConstants.T_INTRO_BOXSET;
        name = "Autocannon/10";
        setInternalName(name);
        addLookupName("IS Auto Cannon/10");
        addLookupName("Auto Cannon/10");
        addLookupName("AutoCannon/10");
        addLookupName("ISAC10");
        addLookupName("IS Autocannon/10");
        heat = 3;
        damage = 10;
        rackSize = 10;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 12.0f;
        criticals = 7;
        bv = 123;
        cost = 200000;
        shortAV = 10;
        medAV = 10;
        maxRange = RANGE_MED;
        explosionDamage = damage;
    }
}
