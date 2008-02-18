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

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.IEntityMovementMode;
import megamek.common.IGame;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public abstract class ArtilleryWeapon extends AmmoWeapon {


    public ArtilleryWeapon() {
        super();
        this.flags |= F_ARTILLERY | F_SPLITABLE;
        this.damage = DAMAGE_ARTILLERY;
    }

    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.IGame,
     *      megamek.server.Server)
     */
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId()).
        getEquipment(waa.getWeaponId()).getLinked().getType();
        if (atype.getMunitionType() == AmmoType.M_HOMING) {
            if (game.getPhase() == IGame.PHASE_FIRING) {
                return new ArtilleryWeaponDirectHomingHandler(toHit, waa, game, server);
            } else  {
                return new ArtilleryWeaponIndirectHomingHandler(toHit, waa, game, server);
            }            
        } else if (game.getPhase() == IGame.PHASE_FIRING) {
            if (waa.getTargetType() == Targetable.TYPE_ENTITY &&
                    atype.getMunitionType() == AmmoType.M_STANDARD &&
                    ((Entity)waa.getTarget(game)).getMovementMode() == IEntityMovementMode.VTOL &&
                    ((Entity)waa.getTarget(game)).getElevation() > 0) {
                return new ArtilleryWeaponFlakHandler(toHit, waa, game, server);
            }
            return new ArtilleryWeaponDirectFireHandler(toHit, waa, game, server);
        } else {
            return new ArtilleryWeaponIndirectFireHandler(toHit, waa, game, server);
        }
    }
}
