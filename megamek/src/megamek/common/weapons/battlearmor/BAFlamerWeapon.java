/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.battlearmor;

import megamek.common.AmmoType;
import megamek.common.BattleForceElement;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.FlamerHandler;
import megamek.common.weapons.Weapon;
import megamek.server.GameManager;

/**
 * @author Andrew Hunter
 * @since Sept 23, 2004
 */
public abstract class BAFlamerWeapon extends Weapon {
    private static final long serialVersionUID = -8198014543155920036L;

    public BAFlamerWeapon() {
        super();
        flags = flags.or(F_FLAMER).or(F_ENERGY).or(F_BA_WEAPON)
                .or(F_BURST_FIRE).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        ammoType = AmmoType.T_NA;
        atClass = CLASS_POINT_DEFENSE;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager manager) {
        return new FlamerHandler(toHit, waa, game, manager);
    }

    @Override
    public int getBattleForceHeatDamage(int range) {
        if (range < BattleForceElement.MEDIUM_RANGE) {
            return getDamage();
        }
        return 0;
    }
}
