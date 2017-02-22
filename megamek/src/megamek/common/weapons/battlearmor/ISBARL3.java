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
import megamek.common.weapons.RLWeapon;


/**
 * @author Sebastian Brocks
 */
public class ISBARL3 extends RLWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5963869448761538363L;

    /**
     *
     */
    public ISBARL3() {
        super();
        name = "Rocket Launcher 3";
        setInternalName("ISBARL3");
        addLookupName("BA RL 3");
        addLookupName("BARL3");
        addLookupName("ISBARocketLauncher3");
        addLookupName("IS BA RLauncher-3");
        rackSize = 3;
        shortRange = 3;
        mediumRange = 7;
        longRange = 12;
        extremeRange = 14;
        bv = 4;
        cost = 4500;
        tonnage = .075f;
        criticals = 3;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).or(F_ONESHOT).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        introDate = 3045;
        techLevel.put(3045, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3050, TechConstants.T_IS_ADVANCED);
        techLevel.put(3052, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_B ,RATING_B};
        techRating = RATING_E;
        rulesRefs = "261, TM";

        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(3045, 3050, 3052);
        techProgression.setTechRating(RATING_E);
        techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_B, RATING_B });
    }
}
