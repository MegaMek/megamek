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
import megamek.common.weapons.SRMWeapon;


/**
 * @author Sebastian Brocks
 */
public class ISBASRM3OS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 8732407650030864483L;

    /**
     *
     */
    public ISBASRM3OS() {
        super();
        name = "SRM 3 (OS)";
        setInternalName("ISBASRM3OS");
        addLookupName("IS BA SRM3 OS");
        rackSize = 3;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        bv = 6;
        cost = 7500;
        tonnage = .125f;
        criticals = 2;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).or(F_ONESHOT).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        introDate = 3045;
        techLevel.put(3045, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3050, TechConstants.T_IS_ADVANCED);
        techLevel.put(3051, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_D ,RATING_B};
        techRating = RATING_E;
        rulesRefs = "261, TM";

        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(3045, 3050, 3051);
        techProgression.setTechRating(RATING_E);
        techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_D, RATING_B });
    }
    
}
