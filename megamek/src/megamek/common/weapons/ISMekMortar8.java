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

import megamek.common.TechConstants;
import megamek.common.TechAdvancement;

/**
 * @author Jason Tighe
 */
public class ISMekMortar8 extends MekMortarWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -3352749710661515958L;

    /**
     *
     */
    public ISMekMortar8() {
        super();

        name = "'Mech Mortar 8";
        setInternalName("IS Mech Mortar-8");
        addLookupName("ISMekMortar8");
        addLookupName("IS Mek Mortar 8");
        rackSize = 8;
        minimumRange = 6;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        bv = 50;
        heat = 10;
        criticals = 5;
        tonnage = 10;
        cost = 70000;
        introDate = 2521;
        extinctDate = 2819;
        reintroDate = 3043;
        techLevel.put(2521, TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(2531, TechConstants.T_IS_ADVANCED);
        techLevel.put(3052, TechConstants.T_IS_TW_NON_BOX);
        availRating = new int[] { RATING_D ,RATING_F ,RATING_F ,RATING_E};
        techRating = RATING_B;
        rulesRefs = "263, TM";
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(2521, 2531, 3052, 2819, 3043);
        techAdvancement.setTechRating(RATING_B);
        techAdvancement.setAvailability( new int[] { RATING_D, RATING_F, RATING_F, RATING_E });
    }
}
