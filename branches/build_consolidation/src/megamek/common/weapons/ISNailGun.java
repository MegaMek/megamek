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
package megamek.common.weapons;

import megamek.common.TechConstants;

/**
 * @author Sebastian Brocks
 */
public class ISNailGun extends NailRivetGunWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5198228513368748633L;

    public ISNailGun() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Nail Gun";
        setInternalName(name);
        addLookupName("ISNailRivet Gun");
        addLookupName("ISNail Gun");
        addLookupName("Nail/Rivet Gun");
        introDate = 2310;
        techLevel.put(2310, techLevel.get(3071));
        availRating = new int[] { RATING_C, RATING_C, RATING_C };
        techRating = RATING_C;
    }
}
