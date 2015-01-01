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
 * Created on Oct 1, 2004
 *
 */
package megamek.common.weapons;

/**
 * @author Andrew Hunter
 */
public class CLUAC2Prototype extends CLPrototypeUACWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -4226217996532332434L;

    /**
     *
     */
    public CLUAC2Prototype() {
        super();
        name = "Ultra AC/2 (CP)";
        setInternalName("CLUltraAC2Prototype");
        heat = 1;
        damage = 2;
        rackSize = 2;
        minimumRange = 3;
        shortRange = 8;
        mediumRange = 17;
        longRange = 25;
        extremeRange = 34;
        tonnage = 7.0f;
        criticals = 4;
        bv = 56;
        cost = 120000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        extAV = 3;
        maxRange = RANGE_EXT;
        explosionDamage = damage;
    }
}
