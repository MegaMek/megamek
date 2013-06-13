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

import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * @author Andrew Hunter
 */
public class ISSmallPulseLaser extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = 2977404162226570144L;

    /**
     * 
     */
    public ISSmallPulseLaser() {
        super();
        this.techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        this.name = "Small Pulse Laser";
        this.setInternalName("ISSmallPulseLaser");
        this.addLookupName("IS Small Pulse Laser");
        this.addLookupName("ISSmall Pulse Laser");
        this.heat = 2;
        this.damage = 3;
        this.infDamageClass = WeaponType.WEAPON_BURST_2D6;
        this.toHitModifier = -2;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.waterShortRange = 1;
        this.waterMediumRange = 2;
        this.waterLongRange = 2;
        this.waterExtremeRange = 4;
        this.tonnage = 1.0f;
        this.criticals = 1;
        this.bv = 12;
        this.cost = 16000;
        this.shortAV = 3;
        this.maxRange = RANGE_SHORT;
        this.atClass = CLASS_POINT_DEFENSE;
        introDate = 2609;
        techLevel.put(2609, techLevel.get(3071));
        extinctDate = 2950;
        reintroDate = 3037;
        availRating = new int[] { RATING_E, RATING_F, RATING_D };
        techRating = RATING_E;
        this.flags = flags.or(F_BURST_FIRE);
    }

}
