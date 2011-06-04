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
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class ISC3RemoteSensorLauncher extends MissileWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -6850419038862085767L;

    /**
     *
     */
    public ISC3RemoteSensorLauncher() {
        super();
        flags = flags.or(F_NO_FIRES);
        ammoType = AmmoType.T_C3_REMOTE_SENSOR;
        cost = 400000;
        criticals = 3;
        tankslots = 1;
        techRating = RATING_E;
        techLevel = TechConstants.T_IS_EXPERIMENTAL;
        bv = 30;
        availRating = new int[]{RATING_X, RATING_X, RATING_F};
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON);
        //suppveeslots = 3;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     *      megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return super.getCorrectHandler(toHit, waa, game, server);
        //FIXME: Implement handler
    }
}
