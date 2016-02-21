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
 * Created on Sep 2, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public abstract class LaserWeapon extends EnergyWeapon {
    /**
     *
     */
    private static final long serialVersionUID = -9210696480919833245L;

    public LaserWeapon() {
        super();
        flags = flags.or(F_LASER).or(F_DIRECT_FIRE);
        ammoType = AmmoType.T_NA;

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
            WeaponAttackAction waa, IGame game, Server server) {
        Mounted linkedBy = waa.getEntity(game).getEquipment(waa.getWeaponId())
                .getLinkedBy();
        if ((linkedBy != null) && !linkedBy.isInoperable()
                && linkedBy.getType().hasFlag(MiscType.F_LASER_INSULATOR)) {
            return new InsulatedLaserWeaponHandler(toHit, waa, game, server);
        }
        return new EnergyWeaponHandler(toHit, waa, game, server);
    }
}
