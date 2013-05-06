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
public class ISNarcIOS extends NarcWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 8610275030183400408L;

    /**
     *
     */
    public ISNarcIOS() {
        super();
        techLevel.put(3071,TechConstants.T_IS_ADVANCED);
        name = "Narc (I-OS)";
        setInternalName("ISNarcBeacon (I-OS)");
        addLookupName("IS I-OS Narc Beacon");
        addLookupName("IS Narc Missile Beacon (I-OS)");
        heat = 0;
        rackSize = 1;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 2.5f;
        criticals = 2;
        flags = flags.or(F_ONESHOT);
        bv = 6;
        cost = 100000;
        techRating = RATING_B;
        availRating = new int[]{RATING_X, RATING_X, RATING_F};
        introDate = 3056;
        techLevel.put(3056,techLevel.get(3071));
    }
}
