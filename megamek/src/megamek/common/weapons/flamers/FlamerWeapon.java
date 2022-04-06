/*
 * MegaMek
 * Copyright (c) 2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons.flamers;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.FlamerHandler;
import megamek.common.weapons.lasers.EnergyWeapon;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * @since Sept 23, 2004
 */
public abstract class FlamerWeapon extends EnergyWeapon {
    private static final long serialVersionUID = -8198014543155920036L;

    public FlamerWeapon() {
        super();
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON).or(F_PROTO_WEAPON).or(F_FLAMER).or(F_BURST_FIRE);
        ammoType = AmmoType.T_NA;
        atClass = CLASS_POINT_DEFENSE;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, Server server) {
        return new FlamerHandler(toHit, waa, game, server);
    }
    
    @Override
    public int getBattleForceHeatDamage(int range) {
        //Clan ER Flamer does damage at medium
        if (getMediumRange() > range) {
            return getDamage();
        }
        return 0;
    }
}
