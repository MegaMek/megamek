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

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISExtendedLRM20 extends ExtendedLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2230366483054553162L;

    /**
     *
     */
    public ISExtendedLRM20() {
        super();
        techLevel = TechConstants.T_IS_EXPERIMENTAL;
        name = "ExtendedLRM 20";
        setInternalName(name);
        addLookupName("IS ExtendedLRM-20");
        addLookupName("ISExtendedLRM20");
        addLookupName("IS ExtendedLRM 20");
        addLookupName("ELRM-20 (THB)");
        heat = 10;
        rackSize = 20;
        minimumRange = 10;
        shortRange = 12;
        mediumRange = 22;
        longRange = 38;
        extremeRange = 44;
        tonnage = 18.0f;
        criticals = 8;
        bv = 268;
        cost = 450000;
        shortAV = 12;
        medAV = 12;
        longAV = 12;
        maxRange = RANGE_LONG;
    }
}
