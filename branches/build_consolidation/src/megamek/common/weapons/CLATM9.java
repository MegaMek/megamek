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
public class CLATM9 extends ATMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3779719958622540629L;

    /**
     *
     */
    public CLATM9() {
        super();
        name = "ATM 9";
        setInternalName("CLATM9");
        addLookupName("Clan ATM-9");
        heat = 6;
        rackSize = 9;
        minimumRange = 4;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 5.0f;
        criticals = 4;
        bv = 147;
        cost = 225000;
        shortAV = 14;
        medAV = 14;
        maxRange = RANGE_MED;
    }
}
