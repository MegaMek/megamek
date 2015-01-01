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
 * @author Jason Tighe
 */
public abstract class BPodWeapon extends AmmoWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 654643305102487115L;

    /**
     *
     */
    public BPodWeapon() {
        super();
        heat = 0;
        damage = 1;
        ammoType = AmmoType.T_BPOD;
        rackSize = 1;
        minimumRange = 0;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        tonnage = 1.0f;
        criticals = 1;
        flags |= F_ONESHOT | F_B_POD | F_BALLISTIC | F_MECH_WEAPON | F_AERO_WEAPON |  F_TANK_WEAPON;
        explosive = true;
        bv = 2;
        cost = 6000;
        explosionDamage = 2;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {
        return new BPodHandler(toHit, waa, game, server);
    }
}
