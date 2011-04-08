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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISLRM3IOS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 435741447089925036L;

    /**
     *
     */
    public ISLRM3IOS() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "LRM 3 (I-OS)";
        setInternalName(name);
        addLookupName("ISLRM3IOS");
        rackSize = 3;
        minimumRange = 6;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        bv = 6;
        tonnage -= .5f;
        flags = flags.or(F_ONESHOT);
    }
}
