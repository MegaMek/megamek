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
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.ASEWMissileWeaponHandler;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.missiles.ThunderBoltWeapon;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class ISASEWMissileWeapon extends ThunderBoltWeapon {

    /**
     * 
     */
    private static final long serialVersionUID = -2094737986722961212L;

    public ISASEWMissileWeapon() {
        super();

        this.name = "Anti-Ship Electronic Warfare (ASEW) Missiles";
        this.setInternalName(BombType.getBombWeaponName(BombType.B_ASEW));
        this.heat = 0;
        this.damage = 0;
        this.rackSize = 1;
        this.minimumRange = 7;
        this.shortRange = 14;
        this.mediumRange = 21;
        this.longRange = 28;
        this.extremeRange = 42;
        this.tonnage = 2;
        this.criticals = 0;
        this.hittable = false;
        this.bv = 0;
        this.cost = 20000;
        this.flags = flags.or(F_MISSILE).or(F_LARGEMISSILE).or(F_BOMB_WEAPON);
        this.shortAV = 0;
        this.medAV = 0;
        this.longAV = 0;
        this.maxRange = RANGE_MED;
        this.ammoType = AmmoType.T_ASEW_MISSILE;
        this.capital = false;
        rulesRefs = "358,TO";
        techAdvancement.setTechBase(TECH_BASE_IS)
    	.setIntroLevel(false)
    	.setUnofficial(false)
        .setTechRating(RATING_E)
        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
        .setISAdvancement(3067, 3073, DATE_NONE, DATE_NONE, DATE_NONE)
        .setISApproximate(false, false, false, false, false)
        .setPrototypeFactions(F_LC)
        .setProductionFactions(F_LC);
    }
    
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new ASEWMissileWeaponHandler(toHit, waa, game, server);
    }
}
