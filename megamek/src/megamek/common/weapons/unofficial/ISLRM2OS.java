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
package megamek.common.weapons.unofficial;

import megamek.common.TechAdvancement;
import megamek.common.weapons.lrms.LRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISLRM2OS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -7559909598872310558L;

    /**
     *
     */
    public ISLRM2OS() {
        super();
        name = "LRM 2 (OS)";
        setInternalName(name);
        addLookupName("ISLRM2OS");
        rackSize = 2;
        minimumRange = 6;
        bv = 4;
        flags = flags.or(F_ONESHOT);
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 3057);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_E, RATING_X });
    }
}
