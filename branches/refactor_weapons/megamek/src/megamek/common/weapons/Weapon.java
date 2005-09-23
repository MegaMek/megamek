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
 * Created on May 10, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter A class representing a weapon.
 */
public abstract class Weapon extends WeaponType {
    public Weapon() {
        this.minimumRange = WEAPON_NA;
    }

    public AttackHandler fire(WeaponAttackAction waa, IGame game, Server server) {
        ToHitData toHit = waa.toHit(game);
        Entity ae = game.getEntity(waa.getEntityId());
        Mounted weapon = ae.getEquipment(waa.getWeaponId());

        return toHit.getValue() == TargetRoll.IMPOSSIBLE ? null
                : getCorrectHandler(toHit, waa, game, server);

    }

    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new WeaponHandler(toHit, waa, game, server);
    }
}
