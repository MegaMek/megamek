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
package megamek.common.weapons.missiles;

import megamek.common.BattleForceElement;
import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.MissileWeaponHandler;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public abstract class MissileWeapon extends AmmoWeapon {

    private static final long serialVersionUID = -2759022204865126991L;

    /**
     *
     */
    public MissileWeapon() {
        super();
        damage = DAMAGE_BY_CLUSTERTABLE;
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON)
                .or(F_AERO_WEAPON).or(F_MISSILE);
        infDamageClass = WEAPON_CLUSTER_MISSILE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, Server server) {
        return new MissileWeaponHandler(toHit, waa, game, server);
    }
    
    @Override
    public double getBattleForceDamage(int range) {
        return getBattleForceDamage(range, null);
    }
    
    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range > getLongRange()) {
            return 0;
        }
        int clusterRoll = 7;
        if (fcs != null && fcs.getType() instanceof MiscType) {
            if (((MiscType)fcs.getType()).hasFlag(MiscType.F_ARTEMIS)) {
                clusterRoll = 9;
            } else if (((MiscType)fcs.getType()).hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                clusterRoll = 8;
            } else if (((MiscType)fcs.getType()).hasFlag(MiscType.F_ARTEMIS_V)) {
                clusterRoll = 10;
            }
        }
        double damage = Compute.calculateClusterHitTableAmount(clusterRoll, getRackSize());
        if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }

    @Override
    public double getBattleForceDamage(int range, int baSquadSize) {
        if (range > getLongRange()) {
            return 0;
        }
        double damage = Compute.calculateClusterHitTableAmount(7, getRackSize() * baSquadSize);
        if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }

}
