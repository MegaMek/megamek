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
public class CLATM12 extends ATMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -7902048944230263372L;

    /**
     *
     */
    public CLATM12() {
        super();
        name = "ATM 12";
        setInternalName("CLATM12");
        addLookupName("Clan ATM-12");
        heat = 8;
        rackSize = 12;
        minimumRange = 4;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 7.0f;
        criticals = 5;
        bv = 212;
        cost = 350000;
        shortAV = 20;
        medAV = 20;
        maxRange = RANGE_MED;
    }
}
