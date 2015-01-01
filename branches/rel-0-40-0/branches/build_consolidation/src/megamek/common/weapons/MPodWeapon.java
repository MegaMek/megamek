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
public abstract class MPodWeapon extends AmmoWeapon {

    private static final long serialVersionUID = 3343394645568467135L;

    /**
     *
     */
    public MPodWeapon() {
        super();
        heat = 0;
        damage = 15;
        ammoType = AmmoType.T_MPOD;
        rackSize = 15;
        minimumRange = 0;
        shortRange = 1;
        mediumRange = 2;
        longRange = 3;
        extremeRange = 4;
        tonnage = 1.0f;
        criticals = 1;
        flags = flags.or(F_MECH_WEAPON).or(F_BALLISTIC).or(F_DIRECT_FIRE)
                .or(F_ONESHOT).or(F_M_POD);
        explosive = true;
        bv = 5;
        cost = 6000;
        explosionDamage = 5;
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
        return new MPodHandler(toHit, waa, game, server);
    }
}
