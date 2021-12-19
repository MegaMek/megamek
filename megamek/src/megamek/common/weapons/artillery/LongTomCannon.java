/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.artillery;

import megamek.common.AmmoType;
import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class LongTomCannon extends ArtilleryCannonWeapon {
    private static final long serialVersionUID = -3643634306982832651L;

    public LongTomCannon() {
        super();

        name = "Long Tom Cannon";
        setInternalName("ISLongTomCannon");
        addLookupName("ISLongTomArtilleryCannon");
        addLookupName("IS Long Tom Cannon");
        addLookupName("CLLongTomCannon");
        addLookupName("CLLongTomArtilleryCannon");
        addLookupName("CL Long Tom Cannon");
        heat = 20;
        rackSize = 20;
        ammoType = AmmoType.T_LONG_TOM_CANNON;
        tonnage = 20;
        criticals = 15;
        bv = 329;
        cost = 650000;
        minimumRange = 4;
        shortRange = 6;
        mediumRange = 13;
        longRange = 20;
        extremeRange = 26;
        shortAV = 20;
        medAV = 20;
        longAV = 20;
        maxRange = RANGE_LONG;
        rulesRefs = "285,TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_B).setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(3012, 3079, DATE_NONE, DATE_NONE,DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3032, 3079, DATE_NONE, DATE_NONE,DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_LC,F_CWF).setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
}
