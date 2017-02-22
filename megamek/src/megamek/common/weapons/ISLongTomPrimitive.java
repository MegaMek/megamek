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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.TechConstants;
import megamek.common.TechProgression;

/**
 * @author Dave Nawton
 */
public class ISLongTomPrimitive extends ArtilleryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 5323886711682442495L;

    /**
     *
     */
    public ISLongTomPrimitive() {
        super();

        name = "Primitive Prototype Long Tom Artillery";
        setInternalName("ISPrimitiveLongTom");
        addLookupName("ISPrimitiveLongTomArtillery");
        heat = 20;
        rackSize = 25;
        ammoType = AmmoType.T_LONG_TOM_PRIM;
        shortRange = 1;
        mediumRange = 2;
        longRange = 30;
        extremeRange = 30; // No extreme range.
        tonnage = 30f;
        criticals = 30;
        bv = 368;
        cost = 450000;
        introDate = 2445;
        extinctDate = 2510;
        techLevel.put(2445, TechConstants.T_IS_EXPERIMENTAL);
        availRating = new int[] { RATING_F ,RATING_X ,RATING_X ,RATING_X};
        techRating = RATING_B;
        rulesRefs = "118, IO";

        techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        techProgression.setISProgression(2445, DATE_NONE, DATE_NONE, 2510);
        techProgression.setTechRating(RATING_B);
        techProgression.setAvailability( new int[] { RATING_F, RATING_X, RATING_X, RATING_X });
    }

}

//TODO - These Long Toms jam on the roll of a two

