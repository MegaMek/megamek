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

import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.VariableSpeedPulseLaserWeaponHandler;
import megamek.server.GameManager;

/**
 * @author Jason Tighe
 */
public class VariableSpeedPulseLaserWeapon extends LaserWeapon {

    private static final long serialVersionUID = -731162221147163665L;

    public VariableSpeedPulseLaserWeapon() {
        super();
        flags = flags.or(F_PULSE).andNot(F_PROTO_WEAPON);
        atClass = CLASS_PULSE_LASER;
        infDamageClass = WEAPON_DIRECT_FIRE;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new VariableSpeedPulseLaserWeaponHandler(toHit, waa, game, manager);
    }

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

}
