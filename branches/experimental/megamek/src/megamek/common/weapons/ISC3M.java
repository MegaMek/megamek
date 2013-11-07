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
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISC3M extends TAGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8367068184993071837L;

    public ISC3M() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "C3 Master with TAG";
        setInternalName("ISC3MasterUnit");
        addLookupName("IS C3 Computer");
        addLookupName("ISC3MasterComputer");
        tonnage = 5;
        criticals = 5;
        hittable = true;
        spreadable = false;
        cost = 1500000;
        bv = 0;
        flags = flags.or(F_C3M);
        heat = 0;
        damage = 0;
        shortRange = 5;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        introDate = 3050;
        techLevel.put(3050, techLevel.get(3071));
    }
}
