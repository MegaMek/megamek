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
package megamek.common.weapons.artillery;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class ISArrowIV extends ArtilleryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -4495524659692575107L;

    /**
     *
     */
    public ISArrowIV() {
        super();
        name = "Arrow IV";
        setInternalName("ISArrowIV");
        addLookupName("ISArrowIVSystem");
        addLookupName("IS Arrow IV System");
        addLookupName("IS Arrow IV Missile System");
        heat = 10;
        rackSize = 20;
        ammoType = AmmoType.T_ARROW_IV;
        shortRange = 1;
        mediumRange = 2;
        longRange = 8;
        extremeRange = 8; // No extreme range.
        tonnage = 15;
        criticals = 15;
        bv = 240;
        cost = 450000;
        this.flags = flags.or(F_MISSILE);
        rulesRefs = "284,TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
            .setTechRating(RATING_E)
            .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
            .setISAdvancement(2593, 2600, 3048, 2830, 3044)
            .setISApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH)
            .setReintroductionFactions(F_CC)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

}
