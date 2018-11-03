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
 * Created on Oct 14, 2004
 *
 */
package megamek.common.weapons.autocannons;

import megamek.common.AmmoType;
import megamek.common.BattleForceElement;
import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.ACWeaponHandler;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.LBXHandler;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public abstract class LBXACWeapon extends AmmoWeapon {

    private static final long serialVersionUID = 5478539267390524833L;

    /**
     *
     */

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
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId())
                .getEquipment(waa.getWeaponId()).getLinked().getType();
        if (atype.getMunitionType() == AmmoType.M_CLUSTER) {
            return new LBXHandler(toHit, waa, game, server);
        }
        return new ACWeaponHandler(toHit, waa, game, server);
    }

    public LBXACWeapon() {
        super();
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON).or(F_PROTO_WEAPON)
                .or(F_BALLISTIC).or(F_DIRECT_FIRE);
        ammoType = AmmoType.T_AC_LBX;
        atClass = CLASS_LBX_AC;
    }
    
    @Override
    public double getBattleForceDamage(int range) {
        double damage = 0;
        if (range <= getLongRange()) {
            damage = Compute.calculateClusterHitTableAmount(7, getRackSize()) / 10.0;
            damage *= 1.05; // -1 to hit
            if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
        }
        return damage;
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_FLAK;
    }
}
