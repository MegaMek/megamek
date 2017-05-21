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
package megamek.common.weapons.battlearmor;

import megamek.common.TechAdvancement;
import megamek.common.weapons.lrms.LRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLBALRM2OS extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5274335014393603612L;

    /**
     *
     */
    public CLBALRM2OS() {
        super();
        name = "LRM 2 (OS)";
        setInternalName("CLBALRM2OS");
        heat = 0;
        rackSize = 2;
        minimumRange = WEAPON_NA;
        tonnage = 0.05;
        criticals = 2;
        bv = 5;
        cost = 6000;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).or(F_ONESHOT).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
		rulesRefs = "261, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(3053, 3060, 3062);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_D });
    }
}
