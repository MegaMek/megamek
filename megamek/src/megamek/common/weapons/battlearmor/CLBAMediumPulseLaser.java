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
 * @author Andrew Hunter
 */
public class CLBAMediumPulseLaser extends PulseLaserWeapon {
    /**
     * 
     */
    private static final long serialVersionUID = -5538336797804604495L;

    /**
     * 
     */
    public CLBAMediumPulseLaser() {
        super();
        this.techLevel.put(3071, TechConstants.T_CLAN_TW);
        this.name = "Medium Pulse Laser";
        this.setInternalName("CLBAMediumPulseLaser");
        this.addLookupName("Clan BA Pulse Med Laser");
        this.addLookupName("Clan BA Medium Pulse Laser");
        this.heat = 4;
        this.damage = 7;
        this.toHitModifier = -2;
        this.shortRange = 4;
        this.mediumRange = 8;
        this.longRange = 12;
        this.extremeRange = 16;
        this.waterShortRange = 3;
        this.waterMediumRange = 5;
        this.waterLongRange = 8;
        this.waterExtremeRange = 10;
        this.tonnage = .8f;
        this.criticals = 3;
        this.bv = 111;
        this.cost = 60000;
        this.shortAV = 7;
        this.medAV = 7;
        this.maxRange = RANGE_MED;
        introDate = 2827;
        techLevel.put(2827, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_D };
        techRating = RATING_F;
        this.flags = flags.or(F_BURST_FIRE).or(F_BA_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
    }
}
