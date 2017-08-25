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
import megamek.common.BombType;
import megamek.common.TechConstants;

/**
 * @author Jay Lawson
 */
public class BombISRL10 extends MissileWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 5763858241912399084L;

    public BombISRL10() {
        super();
        this.techLevel.put(3071, TechConstants.T_IS_TW_NON_BOX);
        this.name = "RL 10 (Bomb)";
        this.setInternalName(BombType.getBombWeaponName(BombType.B_RL));
        flags = flags.or(F_BOMB_WEAPON);
        this.heat = 0;
        this.rackSize = 10;
        this.shortRange = 5;
        this.mediumRange = 11;
        this.longRange = 18;
        this.extremeRange = 22;
        this.tonnage = 1;
        this.criticals = 0;
        this.hittable = false;
        this.bv = 0;
        this.cost = 0;
        this.shortAV = 6;
        this.medAV = 6;
        this.maxRange = RANGE_MED;
        this.toHitModifier = 1;
        this.ammoType = AmmoType.T_RL_BOMB;
        introDate = 3064;
        techLevel.put(3064, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_B };
        techRating = RATING_B;
    }
}
