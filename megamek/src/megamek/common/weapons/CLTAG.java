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

import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class CLTAG extends TAGWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 7446980554102548125L;

    public CLTAG() {
        super();
        name = "Clan TAG";
        setInternalName("CLTAG");
        addLookupName("Clan TAG");
        tonnage = 1;
        criticals = 1;
        hittable = true;
        spreadable = false;
        heat = 0;
        damage = 0;
        shortRange = 5;
        mediumRange = 9;
        longRange = 15;
        extremeRange = 18;
        bv = 0;
        cost = 50000;
        flags = flags.andNot(F_BA_WEAPON).or(F_AERO_WEAPON);
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(DATE_NONE, DATE_NONE, 2820);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_E, RATING_D, RATING_X });
    }
}
