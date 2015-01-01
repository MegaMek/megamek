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

import java.io.Serializable;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter A class representing a weapon.
 */
public abstract class Weapon extends WeaponType implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -8781224279449654544L;

    public Weapon() {
        this.ammoType = AmmoType.T_NA;
        this.minimumRange = WEAPON_NA;
    }

    public AttackHandler fire(WeaponAttackAction waa, IGame game, Server server) {
        ToHitData toHit = waa.toHit(game);
        // FIXME: SUPER DUPER EVIL HACK: swarm missile handlers must be returned
        // even
        // if the have an impossible to hit, because there might be other
        // targets
        // someone else please please figure out how to do this nice
        AttackHandler ah = getCorrectHandler(toHit, waa, game, server);
        if (ah instanceof LRMSwarmHandler || ah instanceof LRMSwarmIHandler)
            return ah;
        return toHit.getValue() == TargetRoll.IMPOSSIBLE ? null
                : ah;
    }

    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new WeaponHandler(toHit, waa, game, server);
    }
}
