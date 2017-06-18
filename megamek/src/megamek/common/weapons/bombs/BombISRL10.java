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
package megamek.common.weapons.bombs;

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.TechAdvancement;
import megamek.common.weapons.MissileWeapon;

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

        this.name = "Rocket Launcher Pod";
        this.setInternalName(BombType.getBombWeaponName(BombType.B_RL));
        addLookupName("RL 10 (Bomb)");
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
        rulesRefs = "229, TM";

        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        techAdvancement.setISAdvancement(3055, 3064, 3067);
        techAdvancement.setTechRating(RATING_B);
        techAdvancement.setAvailability( new int[] { RATING_X, RATING_X, RATING_B, RATING_B });
    }
}
