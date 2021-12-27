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
package megamek.common.weapons.primitive;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;
import megamek.common.weapons.artillery.ArtilleryWeapon;

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
        this.shortName = "Long Tom p";
        heat = 20;
        rackSize = 25;
        ammoType = AmmoType.T_LONG_TOM_PRIM;
        shortRange = 1;
        mediumRange = 2;
        longRange = 30;
        extremeRange = 30; // No extreme range.
        tonnage = 30;
        criticals = 30;
        bv = 368;
        cost = 450000;
        rulesRefs = "118, IO";
        flags = flags.or(F_PROTOTYPE);
        techAdvancement.setTechBase(TECH_BASE_IS)
        .setTechRating(RATING_E)
        .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
        .setISAdvancement(2445, DATE_NONE, DATE_NONE, 2500, DATE_NONE)
        .setISApproximate(false, false, false,true, false)
        .setPrototypeFactions(F_TH)
        .setProductionFactions(F_TH)
        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

}

//TODO - These Long Toms jam on the roll of a two

