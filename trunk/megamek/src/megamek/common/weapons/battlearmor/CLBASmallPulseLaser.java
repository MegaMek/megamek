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
import megamek.common.WeaponType;
import megamek.common.weapons.PulseLaserWeapon;


/**
 * @author Andrew Hunter
 */
public class CLBASmallPulseLaser extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -3257397139779601796L;

    /**
     * 
     */
    public CLBASmallPulseLaser() {
        super();
        this.techLevel.put(3071, TechConstants.T_CLAN_TW);
        this.name = "Small Pulse Laser";
        this.setInternalName("CLBASmallPulseLaser");
        this.addLookupName("Clan BA Pulse Small Laser");
        this.addLookupName("Clan BA Small Pulse Laser");
        this.heat = 2;
        this.damage = 3;
        this.infDamageClass = WeaponType.WEAPON_BURST_2D6;
        this.toHitModifier = -2;
        this.shortRange = 2;
        this.mediumRange = 4;
        this.longRange = 6;
        this.extremeRange = 8;
        this.waterShortRange = 1;
        this.waterMediumRange = 2;
        this.waterLongRange = 4;
        this.waterExtremeRange = 4;
        this.tonnage = .4f;
        this.criticals = 1;
        this.bv = 24;
        this.cost = 16000;
        this.shortAV = 3;
        this.maxRange = RANGE_SHORT;
        this.flags = flags.or(F_BURST_FIRE).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        introDate = 2829;
        techLevel.put(2829, techLevel.get(3071));
        availRating = new int[] { RATING_E, RATING_F, RATING_D };
        techRating = RATING_F;
    }

}
