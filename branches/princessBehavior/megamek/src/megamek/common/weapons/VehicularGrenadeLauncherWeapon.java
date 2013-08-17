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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public abstract class VehicularGrenadeLauncherWeapon extends AmmoWeapon {

    private static final long serialVersionUID = 3343394645568467135L;

    /**
     *
     */
    public VehicularGrenadeLauncherWeapon() {
        super();
        heat = 1;
        damage = 0;
        ammoType = AmmoType.T_VGL;
        rackSize = 1;
        minimumRange = 0;
        shortRange = 1;
        mediumRange = 1;
        longRange = 1;
        extremeRange = 1;
        tonnage = 0.5f;
        criticals = 1;
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_BALLISTIC)
                .or(F_ONESHOT).or(F_VGL);
        explosive = false;
        bv = 15;
        cost = 10000;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        // FIXME: implement handlers for the 4 ammo types:
        // standard (fragmentation), chaff, incendiary, smoke
        return new AmmoWeaponHandler(toHit, waa, game, server);
    }
}
