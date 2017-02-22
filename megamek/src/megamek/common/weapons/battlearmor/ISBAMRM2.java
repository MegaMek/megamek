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
import megamek.common.TechProgression;
import megamek.common.weapons.MRMWeapon;

/**
 * @author Sebastian Brocks
 */
public class ISBAMRM2 extends MRMWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -8616767319138511565L;

    /**
     * 
     */
    public ISBAMRM2() {
        super();
        this.name = "MRM 2";
        this.setInternalName("ISBAMRM2");
        this.addLookupName("BA MRM-2");
        this.addLookupName("IS BA MRM 2");
        this.rackSize = 2;
        this.shortRange = 3;
        this.mediumRange = 8;
        this.longRange = 15;
        this.extremeRange = 16;
        this.bv = 9;
        cost = 10000;
        tonnage = .12f;
        criticals = 2;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        introDate = 3053;
        techLevel.put(3053, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3060, TechConstants.T_IS_ADVANCED);
        techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_D ,RATING_D};
        techRating = RATING_D;
        rulesRefs = "261, TM";

        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(3053, 3060, 3067);
        techProgression.setTechRating(RATING_D);
        techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_D, RATING_D });
    }
}
