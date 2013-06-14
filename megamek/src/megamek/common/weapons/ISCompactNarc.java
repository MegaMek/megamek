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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISCompactNarc extends NarcWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 6784282679924023973L;

    /**
     *
     */
    public ISCompactNarc() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Compact Narc";
        setInternalName("ISCompactNarc");
        addLookupName("ISCompact Narc");
        heat = 0;
        rackSize = 4;
        shortRange = 2;
        mediumRange = 4;
        longRange = 5;
        extremeRange = 8;
        tonnage = 0.0f;
        criticals = 0;
        bv = 16;
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC);
    }

}
