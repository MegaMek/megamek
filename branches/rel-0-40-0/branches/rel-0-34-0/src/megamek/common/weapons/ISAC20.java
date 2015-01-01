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
public class ISAC20 extends ACWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 4780847244648362671L;

    /**
     * 
     */
    public ISAC20() {
        super();
        techLevel = TechConstants.T_INTRO_BOXSET;
        name = "Autocannon/20";
        setInternalName(name);
        addLookupName("IS Auto Cannon/20");
        addLookupName("Auto Cannon/20");
        addLookupName("AutoCannon/20");
        addLookupName("ISAC20");
        addLookupName("IS Autocannon/20");
        heat = 7;
        damage = 20;
        rackSize = 20;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 14.0f;
        criticals = 10;
        bv = 178;
        flags |= F_SPLITABLE;
        cost = 300000;
        shortAV = 20;
        maxRange = RANGE_SHORT;
        explosionDamage = damage;
    }
}
