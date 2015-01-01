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
        techLevel = TechConstants.T_INTRO_BOXSET;
        name = "Autocannon/5";
        setInternalName(name);
        addLookupName("IS Auto Cannon/5");
        addLookupName("Auto Cannon/5");
        addLookupName("AutoCannon/5");
        addLookupName("ISAC5");
        addLookupName("IS Autocannon/5");
        heat = 1;
        damage = 5;
        rackSize = 5;
        minimumRange = 3;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        tonnage = 8.0f;
        criticals = 4;
        bv = 70;
        cost = 125000;
        shortAV = 5;
        medAV = 5;
        maxRange = RANGE_MED;
        explosionDamage = damage;
    }
}
