/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
/*
 * Created on Sep 25, 2004
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
        techLevel.put(3071,TechConstants.T_IS_UNOFFICIAL);
        name = "LAC/20";
        setInternalName("Light Auto Cannon/20");
        addLookupName("IS Light AutoCannon/20");
        addLookupName("ISLAC20");
        addLookupName("IS Light Autocannon/20");
        heat = 7;
        damage = 20;
        rackSize = 20;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        tonnage = 9.0f;
        criticals = 6;
        bv = 118;
        cost = 325000;
        explosionDamage = damage;
    }
}
