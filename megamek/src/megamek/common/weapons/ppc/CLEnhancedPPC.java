/*
 * MegaMek - Copyright (c) 2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.ppc;

import megamek.common.SimpleTechLevel;

/**
 * @author Harold "BATTLEMASTER" N.
 * @since Sep 13, 2004
 */
public class CLEnhancedPPC extends PPCWeapon {
    private static final long serialVersionUID = 5108976056064542099L;

    public CLEnhancedPPC() {
        super();
        this.name = "Enhanced PPC";
        this.setInternalName("CLWERPPC");
        this.addLookupName("Wolverine ER PPC");
        this.addLookupName("CLWERPPC");
        this.addLookupName("Wolverine ER PPC");
        this.addLookupName("ISEHERPPC");
        this.addLookupName("IS EH ER PPC");
        sortingName = "PPC Enhanced";
        this.heat = 15;
        this.damage = 12;
        this.shortRange = 7;
        this.mediumRange = 14;
        this.longRange = 23;
        this.extremeRange = 28;
        this.waterShortRange = 4;
        this.waterMediumRange = 10;
        this.waterLongRange = 16;
        this.waterExtremeRange = 20;
        this.tonnage = 7.0;
        this.criticals = 3;
        this.bv = 329;
        this.cost = 300000;
        this.shortAV = 12;
        this.medAV = 12;
        this.longAV = 12;
        this.maxRange = RANGE_LONG;
        rulesRefs = "95, IO";
        techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_E, RATING_X, RATING_E)
                .setClanAdvancement(2822, 2823, DATE_NONE, 2831, 3080)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CWV)
                .setProductionFactions(F_CWV)
                .setReintroductionFactions(F_EI)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }
}
