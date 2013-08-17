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
package megamek.common.weapons;

/**
 * @author Sebastian Brocks
 */
public class CLATM3 extends ATMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 107949833660086492L;

    /**
     *
     */
    public CLATM3() {
        super();
        name = "ATM 3";
        setInternalName("CLATM3");
        addLookupName("Clan ATM-3");
        heat = 2;
        rackSize = 3;
        minimumRange = 4;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 1.5f;
        criticals = 2;
        bv = 53;
        cost = 50000;
        shortAV = 4;
        medAV = 4;
        maxRange = RANGE_MED;
    }
}
