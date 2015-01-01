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
package megamek.common.weapons.battlearmor;

import megamek.common.TechConstants;
import megamek.common.weapons.PulseLaserWeapon;

/**
 * @author Jay Lawson
 */
public class ISBAMediumPulseLaser extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 2676144961105838316L;

    /**
     * 
     */
    public ISBAMediumPulseLaser() {
        super();
        this.techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        this.name = "BA Medium Pulse Laser";
        this.setInternalName("ISBAMediumPulseLaser");
        this.addLookupName("IS BA Pulse Med Laser");
        this.addLookupName("IS BA Medium Pulse Laser");
        this.damage = 6;
        this.toHitModifier = -2;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.waterShortRange = 2;
        this.waterMediumRange = 3;
        this.waterLongRange = 4;
        this.waterExtremeRange = 6;
        this.tonnage = 0.8f;
        this.criticals = 3;
        this.bv = 48;
        this.cost = 60000;
        flags = flags.or(F_BA_WEAPON);
        this.shortAV = 6;
        this.maxRange = RANGE_SHORT;
        introDate = 2609;
        techLevel.put(2609, techLevel.get(3071));
        extinctDate = 2950;
        reintroDate = 3037;
        availRating = new int[] { RATING_X, RATING_X, RATING_D };
        techRating = RATING_E;

    }
}
