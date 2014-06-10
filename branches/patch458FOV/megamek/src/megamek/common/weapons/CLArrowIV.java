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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class CLArrowIV extends ArtilleryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -8623816593973861926L;

    /**
     *
     */
    public CLArrowIV() {
        super();
        techLevel.put(3071, TechConstants.T_CLAN_ADVANCED);
        name = "Arrow IV";
        setInternalName("CLArrowIV");
        addLookupName("CLArrowIVSystem");
        addLookupName("Clan Arrow IV System");
        addLookupName("Clan Arrow IV Missile System");
        heat = 10;
        rackSize = 20;
        ammoType = AmmoType.T_ARROW_IV;
        shortRange = 1; //
        mediumRange = 2;
        longRange = 9;
        extremeRange = 9; // No extreme range.
        tonnage = 12f;
        criticals = 12;
        bv = 240;
        cost = 450000;
        techRating = RATING_F;
        availRating = new int[] { RATING_X, RATING_E, RATING_D };
        introDate = 2850;
        techLevel.put(2850, techLevel.get(3071));

    }

}
