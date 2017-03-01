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

import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class CLLRM18 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 4616668322148799167L;

    /**
     *
     */
    public CLLRM18() {
        super();

        name = "LRM 18";
        setInternalName("CLLRM18");
        heat = 0;
        rackSize = 18;
        minimumRange = WEAPON_NA;
        tonnage = 3.6f;
        criticals = 0;
        bv = 217;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        rulesRefs = "231, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(3050, 3059, 3062);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_D });
    }
}
