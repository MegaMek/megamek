/**
 * MegaMek -
 * Copyright (C) 2013 Ben Mazur (bmazur@sev.org)
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

import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

public class ReengineeredLaserWeapon extends LaserWeapon {


    /**
     *
     */
    private static final long serialVersionUID = 2113437667446946251L;

    public ReengineeredLaserWeapon() {
        super();
        techLevel.put(3120,TechConstants.T_IS_EXPERIMENTAL);
        techLevel.put(3130,TechConstants.T_IS_ADVANCED);
        techRating = RATING_E;
        availRating= new int[] {RATING_X,RATING_X,RATING_X,RATING_E};
        introDate = 3120;
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
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {

        return new ReengineeredLaserWeaponHandler(toHit, waa, game, server);
    }
}
