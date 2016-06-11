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

import megamek.common.TechConstants;
import megamek.common.weapons.AdvancedSRMWeapon;
/**
 * @author Sebastian Brocks
 */
public class CLAdvancedSRM3OS extends AdvancedSRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 6946007011975098588L;

    /**
     *
     */
    public CLAdvancedSRM3OS() {
        super();
        name = "Advanced SRM 3 (OS)";
        setInternalName("CLAdvancedSRM3OS");
        rackSize = 3;
        shortRange = 4;
        mediumRange = 8;
        longRange = 12;
        extremeRange = 16;
        bv = 9;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).or(F_ONESHOT).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        tonnage = .105f;
        criticals = 4;
        cost = 22500;
        introDate = 3047;
        techLevel.put(3047, TechConstants.T_CLAN_EXPERIMENTAL);	
        techLevel.put(3056, TechConstants.T_CLAN_ADVANCED);	
        techLevel.put(3062, TechConstants.T_CLAN_TW);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_F ,RATING_D};	
        techRating = RATING_F;

    }
}
