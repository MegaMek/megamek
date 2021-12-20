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
package megamek.common.weapons.lrms;

import megamek.common.SimpleTechLevel;

/**
 * @author BATTLEMASTER
 */
public class ISEnhancedLRM10 extends EnhancedLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3287950524687857609L;

    /**
     *
     */
    public ISEnhancedLRM10() {
        super();
        name = "Enhanced LRM 10";
        setInternalName(name);
        addLookupName("ISEnhancedLRM10");
        heat = 4;
        rackSize = 10;
        minimumRange = 3;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 6.0;
        criticals = 4;
        bv = 104;
        cost = 125000;
        shortAV = 6;
        medAV = 6;
        longAV = 6;
        maxRange = RANGE_LONG;
        rulesRefs = "326,TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS        
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
            .setAvailability(RATING_C, RATING_F, RATING_E, RATING_D)
            .setISAdvancement(3058, DATE_NONE, 3082).setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
