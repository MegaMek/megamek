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
public class ISExtendedLRM15 extends ExtendedLRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -7039029686193601958L;

    /**
     *
     */
    public ISExtendedLRM15() {
        super();
        name = "Extended LRM 15";
        setInternalName(name);
        addLookupName("IS Extended LRM-15");
        addLookupName("ISExtendedLRM15");
        addLookupName("IS Extended LRM 15");
        addLookupName("ELRM-15 (THB)");
        heat = 8;
        rackSize = 15;
        tonnage = 12.0;
        criticals = 6;
        bv = 200;
        cost = 350000;
        shortAV = 9;
        medAV = 9;
        longAV = 9;
        extAV = 9;
        rulesRefs = "327,TO";
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
        .setISAdvancement(3054, 3078, 3083, DATE_NONE, DATE_NONE).setPrototypeFactions(F_FS,F_LC)
        .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
}
