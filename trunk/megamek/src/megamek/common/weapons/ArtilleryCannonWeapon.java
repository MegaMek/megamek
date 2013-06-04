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
/*
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public abstract class ArtilleryCannonWeapon extends AmmoWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -732023379991213890L;

    public ArtilleryCannonWeapon() {
        super();
        damage = DAMAGE_ARTILLERY;
        flags = flags.or(F_BALLISTIC).or(F_MECH_WEAPON).or(F_AERO_WEAPON)
                .or(F_TANK_WEAPON);
        atClass = CLASS_AC;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.IGame,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        // AmmoType atype = (AmmoType)
        // game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId()).getLinked().getType();
        return new ArtilleryCannonWeaponHandler(toHit, waa, game, server);
    }
}
