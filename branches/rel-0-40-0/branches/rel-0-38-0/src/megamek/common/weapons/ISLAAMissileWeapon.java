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
public class ISLAAMissileWeapon extends CapitalMissileWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = 6262048986109960442L;

    public ISLAAMissileWeapon() {
        super();
        this.techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        this.name = "LAA Missile";
        this.setInternalName(BombType.getBombWeaponName(BombType.B_LAA));
        this.heat = 0;
        this.damage = 6;
        this.rackSize = 1;
        this.minimumRange = 7;
        this.shortRange = 14;
        this.mediumRange = 21;
        this.longRange = 28;
        this.extremeRange = 42;
        this.tonnage = 0.5f;
        this.criticals = 0;
        this.hittable = false;
        this.bv = 0;
        this.cost = 6000;
        this.shortAV = 6;
        this.medAV = 6;
        this.maxRange = RANGE_MED;
        this.ammoType = AmmoType.T_LAA_MISSILE;
        this.capital = false;
        introDate = 3072;
        techLevel.put(3072, techLevel.get(3071));
        availRating = new int[] { RATING_X, RATING_X, RATING_F };
        techRating = RATING_D;
    }
}
