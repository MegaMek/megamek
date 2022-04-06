/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.lasers;

import megamek.common.BattleForceElement;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.VariableSpeedPulseLaserWeaponHandler;
import megamek.server.Server;

/**
 * @author Jason Tighe
 */
public class VariableSpeedPulseLaserWeapon extends LaserWeapon {
    private static final long serialVersionUID = -731162221147163665L;

    public VariableSpeedPulseLaserWeapon() {
        super();
        flags = flags.or(F_PULSE);
        atClass = CLASS_PULSE_LASER;
        infDamageClass = WEAPON_DIRECT_FIRE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              Server server) {
        return new VariableSpeedPulseLaserWeaponHandler(toHit, waa, game, server);
    }

    /*
     * 
     * (non-Javadoc)
     * 
     * @see megamek.common.WeaponType#getDamage(int)
     */
    @Override
    public int getDamage(int range) {
        if (range <= shortRange) {
            return damageShort;
        }

        if (range <= mediumRange) {
            return damageMedium;
        }

        return damageLong;
    }

    @Override
    public double getBattleForceDamage(int range) {
        double damage = 0;
        if (range <= getLongRange()) {
            //Variable damage weapons that cannot reach into the BF long range band use LR damage for the MR band
            if (range == BattleForceElement.MEDIUM_RANGE
                    && getLongRange() < BattleForceElement.LONG_RANGE) {
                damage = getDamage(BattleForceElement.LONG_RANGE);
            } else {
                damage = getDamage(range);
            }
            if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
            //To hit mods vary with range
            if (range <= getShortRange()) {
                damage *= 1.15;
            } else if (range <= getMediumRange()) {
                damage *= 1.10;
            } else {
                damage *= 1.05;
            }
        }
        return damage / 10.0;
    }
}
