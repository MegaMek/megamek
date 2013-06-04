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
package megamek.common.weapons;

import megamek.common.EquipmentType;
import megamek.common.TechConstants;

/**
 * @author Andrew Hunter
 */
public class ISLargePulseLaser extends PulseLaserWeapon {
    /**
     *
     */
    private static final long serialVersionUID = 94533476706680275L;

    /**
     *
     */
    public ISLargePulseLaser() {
        super();
        techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        name = "Large Pulse Laser";
        setInternalName("ISLargePulseLaser");
        addLookupName("IS Pulse Large Laser");
        addLookupName("IS Large Pulse Laser");
        heat = 10;
        damage = 9;
        toHitModifier = -2;
        shortRange = 3;
        mediumRange = 7;
        longRange = 10;
        extremeRange = 14;
        waterShortRange = 2;
        waterMediumRange = 5;
        waterLongRange = 7;
        waterExtremeRange = 10;
        tonnage = 7.0f;
        criticals = 2;
        bv = 119;
        cost = 175000;
        shortAV = 9;
        medAV = 9;
        maxRange = RANGE_MED;
        introDate = 2609;
        techLevel.put(2609, techLevel.get(3071));
        extinctDate = 2950;
        reintroDate = 3037;
        availRating = new int[] { RATING_E, RATING_F, RATING_D };
    }
}
