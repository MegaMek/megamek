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
package megamek.common.weapons.lrms;

import megamek.common.SimpleTechLevel;

/**
 * @author Sebastian Brocks
 */
public class ISExtendedLRM5 extends ExtendedLRMWeapon {

    private static final long serialVersionUID = -6153832907941260136L;

    public ISExtendedLRM5() {
        super();
        name = "Extended LRM 5";
        setInternalName(name);
        addLookupName("IS Extended LRM-5");
        addLookupName("ISExtendedLRM5");
        addLookupName("IS Extended LRM 5");
        addLookupName("ELRM-5 (THB)");
        heat = 3;
        rackSize = 5;
        tonnage = 6.0;
        criticals = 1;
        bv = 67;
        cost = 60000;
        shortAV = 3;
        medAV = 3;
        longAV = 3;
        extAV = 3;
        rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(DATE_NONE, 3054, 3080, DATE_NONE, DATE_NONE)
            .setPrototypeFactions(F_FS,F_LC)
            .setProductionFactions(F_LC)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
