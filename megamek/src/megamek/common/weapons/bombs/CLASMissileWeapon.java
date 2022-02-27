/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.bombs;

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.SimpleTechLevel;
import megamek.common.weapons.missiles.ThunderBoltWeapon;

/**
 * @author Jay Lawson
 */
public class CLASMissileWeapon extends ThunderBoltWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 8263429182520693147L;

    public CLASMissileWeapon() {
        super();
        name = "AS Missile";
        setInternalName(BombType.getBombWeaponName(BombType.B_AS));
        heat = 0;
        damage = 30;
        rackSize = 1;
        minimumRange = 9;
        shortRange = 17;
        mediumRange = 25;
        longRange = 32;
        extremeRange = 50;
        tonnage = 2;
        criticals = 0;
        hittable = false;
        bv = 0;
        cost = 15000;
        shortAV = 30;
        medAV = 30;
        longAV = 30;
        flags = flags.or(F_ANTI_SHIP).or(F_MISSILE).or(F_LARGEMISSILE).or(F_BOMB_WEAPON).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON);
        maxRange = RANGE_LONG;
        ammoType = AmmoType.T_AS_MISSILE;
        capital = false;
        this.missileArmor = 30;
        rulesRefs = "358, TO";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_D)
        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
        .setISAdvancement(DATE_NONE, DATE_NONE, 3076, DATE_NONE, DATE_NONE)
        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }
    
}
