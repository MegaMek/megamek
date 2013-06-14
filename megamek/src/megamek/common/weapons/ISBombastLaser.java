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
 * Created on Sep 12, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jason Tighe
 */
public class ISBombastLaser extends LaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = 3379805005243042138L;

    public ISBombastLaser() {
        super();
        techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        name = "Bombast Laser";
        setInternalName(name);
        addLookupName("IS Bombast Laser");
        addLookupName("ISBombastLaser");
        heat = 12;
        damage = 12;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 7.0f;
        criticals = 3;
        bv = 137;
        cost = 200000;
        shortAV = 12;
        medAV = 12;
        maxRange = RANGE_MED;
        flags = flags.or(F_BOMBAST_LASER);
        techRating = RATING_E;
        availRating = new int[] { RATING_X, RATING_X, RATING_E };
        introDate = 3064;
        techLevel.put(3064, techLevel.get(3071));
        techLevel.put(3085, TechConstants.T_IS_ADVANCED);
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
        return new BombastLaserWeaponHandler(toHit, waa, game, server);
    }

}
