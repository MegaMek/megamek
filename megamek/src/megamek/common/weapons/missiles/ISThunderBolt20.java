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
package megamek.common.weapons.missiles;

import megamek.common.AmmoType;

/**
 * @author Sebastian Brocks
 */
public class ISThunderBolt20 extends ThunderBoltWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6976091682813292840L;

    /**
     *
     */
    public ISThunderBolt20() {
        super();
        name = "Thunderbolt 20";
        setInternalName(name);
        addLookupName("IS Thunderbolt-20");
        addLookupName("ISThunderbolt20");
        addLookupName("ISTBolt20");
        addLookupName("IS Thunderbolt 20");
        ammoType = AmmoType.T_TBOLT_20;
        heat = 8;
        minimumRange = 5;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        shortAV = 20;
        medAV = 20;
        maxRange = RANGE_MED;
        tonnage = 15.0;
        criticals = 5;
        bv = 305;
        cost = 450000;
        flags = flags.or(F_LARGEMISSILE);
        rulesRefs = "347,TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(3052, 3072, 3081, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, false,false, false)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS,F_LC);
    }
}
