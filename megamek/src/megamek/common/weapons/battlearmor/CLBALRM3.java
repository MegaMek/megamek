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
import megamek.common.weapons.LRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class CLBALRM3 extends LRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -4086505975056019860L;

    /**
     *
     */
    public CLBALRM3() {
        super();
        name = "LRM 3";
        setInternalName("CLBALRM3");
        heat = 0;
        rackSize = 3;
        minimumRange = WEAPON_NA;
        tonnage = 0.105f;
        criticals = 3;
        bv = 35;
        cost = 18000;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        introDate = 3053;
		techLevel.put(3053, TechConstants.T_CLAN_EXPERIMENTAL);	
		techLevel.put(3060, TechConstants.T_CLAN_ADVANCED);
		techLevel.put(3062, TechConstants.T_CLAN_TW);	
		availRating = new int[] { RATING_X ,RATING_X ,RATING_F ,RATING_D};	
		techRating = RATING_F;	
		rulesRefs = "261, TM";
    }
}
