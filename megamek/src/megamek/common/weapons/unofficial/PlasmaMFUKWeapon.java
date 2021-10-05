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
/*
 * Created on Sep 21, 2005
 *
 */
package megamek.common.weapons.unofficial;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PlasmaMFUKWeaponHandler;
import megamek.common.weapons.lasers.EnergyWeapon;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public abstract class PlasmaMFUKWeapon extends EnergyWeapon {

    private static final long serialVersionUID = -6903718412622554494L;

    /**
     *
     */
    public PlasmaMFUKWeapon() {
        super();
        flags = flags.or(F_DIRECT_FIRE).or(F_PLASMA_MFUK).or(F_ENERGY);
        ammoType = AmmoType.T_NA;
        atClass = CLASS_PLASMA;
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
        return new PlasmaMFUKWeaponHandler(toHit, waa, game, server);
    }

}
