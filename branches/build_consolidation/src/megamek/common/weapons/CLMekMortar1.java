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
 * @author Jason Tighe
 */
public class CLMekMortar1 extends MekMortarWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2449264496450109574L;

    /**
     *
     */
    public CLMekMortar1() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        name = "Mortar 1";
        setInternalName("Clan Mech Mortar-1");
        addLookupName("CLMekMortar1");
        addLookupName("Clan Mek Mortar 1");
        rackSize = 1;
        minimumRange = 6;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        bv = 10;
        heat = 1;
        criticals = 1;
        tonnage = 1;
        cost = 7000;
        techRating = RATING_B;
        availRating = new int[] { RATING_D, RATING_F, RATING_E };
        introDate = 2840;
        techLevel.put(2840, techLevel.get(3071));

    }
}
