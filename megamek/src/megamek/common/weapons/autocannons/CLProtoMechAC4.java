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
package megamek.common.weapons.autocannons;

import megamek.common.SimpleTechLevel;

/**
 * @author Jason Tighe
 * @since Oct 2, 2004
 */
public class CLProtoMechAC4 extends ProtoMechACWeapon {
    private static final long serialVersionUID = 4371171653960292873L;

    public CLProtoMechAC4() {
        super();

        name = "ProtoMech AC/4";
        setInternalName("CLProtoMechAC4");
        addLookupName("Clan ProtoMech AC/4");
        heat = 1;
        damage = 4;
        rackSize = 4;
        minimumRange = 0;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        tonnage = 4.5;
        criticals = 3;
        bv = 49;
        cost = 133000;
        shortAV = 4;
        medAV = 4;
        longAV = 4;
        maxRange = RANGE_MED;
        explosionDamage = damage;
        rulesRefs = "286,TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3070, 3073, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_CBS).setProductionFactions(F_CBS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }
}
