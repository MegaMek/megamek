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

import megamek.common.AmmoType;
import megamek.common.TechAdvancement;

/**
 * @author Sebastian Brocks
 */
public class ISThunderBolt10 extends ThunderBoltWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 6781882739979127656L;

    /**
     *
     */
    public ISThunderBolt10() {
        super();
        name = "Thunderbolt 10";
        setInternalName(name);
        addLookupName("IS Thunderbolt-10");
        addLookupName("ISThunderbolt10");
        addLookupName("IS Thunderbolt 10");
        addLookupName("ISTBolt10");
        ammoType = AmmoType.T_TBOLT_10;
        heat = 5;
        minimumRange = 5;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        shortAV = 10;
        medAV = 10;
        maxRange = RANGE_MED;
        tonnage = 7.0f;
        criticals = 2;
        bv = 127;
        cost = 175000;
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(DATE_NONE, 3072, 3081);
        techAdvancement.setTechRating(RATING_E);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_X });
    }
}
