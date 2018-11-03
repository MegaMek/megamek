package megamek.common.weapons.lasers;

import megamek.common.SimpleTechLevel;

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
 * Created on Sep 12, 2004
 *
 */

/**
 * @author Jason Tighe
 */
public class ISVariableSpeedPulseLaserMedium extends
        VariableSpeedPulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 2676144961105838316L;

    /**
     *
     */
    public ISVariableSpeedPulseLaserMedium() {
        super();
        name = "Medium VSP Laser";
        setInternalName("ISMediumVSPLaser");
        addLookupName("ISMVSPL");
        addLookupName("ISMediumVariableSpeedLaser");
        addLookupName("ISMediumVSP");
        heat = 7;
        damage = DAMAGE_VARIABLE;
        toHitModifier = -4;
        shortRange = 2;
        mediumRange = 5;
        longRange = 9;
        extremeRange = 10;
        waterShortRange = 1;
        waterMediumRange = 3;
        waterLongRange = 6;
        waterExtremeRange = 6;
        damageShort = 9;
        damageMedium = 7;
        damageLong = 5;
        tonnage = 4.0;
        criticals = 2;
        bv = 56;
        cost = 200000;
        shortAV = 7;
        maxRange = RANGE_SHORT;
        rulesRefs = "321,TO";
        techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(3070, 3072, 3080).setPrototypeFactions(F_FW,F_WB)
            .setProductionFactions(F_FW,F_WB).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

}
