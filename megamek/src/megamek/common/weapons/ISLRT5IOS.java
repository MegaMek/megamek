/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons;

import megamek.common.TechConstants;
import megamek.common.TechProgression;

/**
 * @author Sebastian Brocks
 */
public class ISLRT5IOS extends LRTWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -7475019239065402296L;

    /**
     *
     */
    public ISLRT5IOS() {
        super();

        name = "LRT 5 (I-OS)";
        setInternalName(name);
        addLookupName("IS IOS LRT-5");
        addLookupName("ISLRTorpedo5 (IOS)");
        addLookupName("IS LRT 5 (IOS)");
        heat = 2;
        rackSize = 5;
        minimumRange = 6;
        waterShortRange = 7;
        waterMediumRange = 14;
        waterLongRange = 21;
        waterExtremeRange = 28;
        tonnage = 1.5f;
        criticals = 1;
        bv = 9;
        flags = flags.or(F_ONESHOT);
        cost = 24000;
        introDate = 3056;
        techLevel.put(3056, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3076, TechConstants.T_IS_ADVANCED);
        techLevel.put(3085, TechConstants.T_INTRO_BOXSET);
        availRating = new int[] { RATING_X ,RATING_X ,RATING_F ,RATING_E};
        techRating = RATING_B;
        rulesRefs = "230, TM";
        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(3056, 3076, 3085);
        techProgression.setTechRating(RATING_B);
        techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_E });
    }
}
