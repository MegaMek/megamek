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
public class ISExtendedLRM5 extends ExtendedLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6153832907941260136L;

    /**
     *
     */
    public ISExtendedLRM5() {
        super();
        techLevel = TechConstants.T_IS_EXPERIMENTAL;
        name = "ExtendedLRM 5";
        setInternalName(name);
        addLookupName("IS ExtendedLRM-5");
        addLookupName("ISExtendedLRM5");
        addLookupName("IS ExtendedLRM 5");
        addLookupName("ELRM-5 (THB)");
        heat = 3;
        rackSize = 5;
        minimumRange = 10;
        shortRange = 12;
        mediumRange = 22;
        longRange = 38;
        extremeRange = 44;
        tonnage = 6.0f;
        criticals = 1;
        bv = 367;
        cost = 110000;
    }
}
