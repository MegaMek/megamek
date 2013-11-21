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
package megamek.common.weapons.battlearmor;

import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * @author Sebastian Brocks
 */
public class ISBAMG extends BAMGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -4420620461776813639L;

    /**
     *
     */
    public ISBAMG() {
        super();
        techLevel.put(3071, TechConstants.T_INTRO_BOXSET);
        name = "Machine Gun";
        setInternalName("BA Machine Gun");
        addLookupName("IS BA Machine Gun");
        addLookupName("ISBAMachine Gun");
        heat = 0;
        damage = 2;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        rackSize = 2;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 0.1f;
        criticals = 1;
        bv = 5;
        cost = 5000;
        introDate = 1950;
        techLevel.put(1950, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_B };
        techRating = RATING_C;
    }

}
