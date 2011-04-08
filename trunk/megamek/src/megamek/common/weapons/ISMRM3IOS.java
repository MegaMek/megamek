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
public class ISMRM3IOS extends MRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2684723412113127349L;

    /**
     *
     */
    public ISMRM3IOS() {
        super();
        techLevel = TechConstants.T_IS_TW_NON_BOX;
        name = "MRM 3 (I-OS)";
        setInternalName(name);
        addLookupName("ISMRM3IOS");
        rackSize = 3;
        shortRange = 3;
        mediumRange = 8;
        longRange = 15;
        extremeRange = 22;
        bv = 4;
        tonnage -= .5f;
        flags = flags.or(F_ONESHOT);
    }
}
