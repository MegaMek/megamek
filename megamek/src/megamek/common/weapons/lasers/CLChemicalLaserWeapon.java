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

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.ChemicalLaserHandler;
import megamek.server.Server;

/**
 * @author Jason Tighe
 * @since May 29, 2004
 */
public abstract class CLChemicalLaserWeapon extends AmmoWeapon {
    private static final long serialVersionUID = -854810886500324094L;

    public CLChemicalLaserWeapon() {
        ammoType = AmmoType.T_CHEMICAL_LASER;
        minimumRange = WEAPON_NA;
        flags = flags.or(F_DIRECT_FIRE).or(F_ENERGY).or(F_MECH_WEAPON)
                .or(F_TANK_WEAPON).or(F_AERO_WEAPON).or(F_PROTO_WEAPON);
        atClass = CLASS_LASER;
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
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, Server server) {
        return new ChemicalLaserHandler(toHit, waa, game, server);
    }
}
