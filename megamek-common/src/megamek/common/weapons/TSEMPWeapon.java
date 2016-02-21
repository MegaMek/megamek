/**
 * MegaMek - Copyright (C) 2013 Ben Mazur (bmazur@sev.org)
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
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * Tight-Stream Electro-Magnetic Pulse (TSEMP) weapon.  Found in FM:3145 pg 255.
 * 
 * @author arlith
 *
 */
public class TSEMPWeapon extends EnergyWeapon {

    public static int TSEMP_EFFECT_NONE = 0;
    public static int TSEMP_EFFECT_INTERFERENCE = 1;
    public static int TSEMP_EFFECT_SHUTDOWN = 2;

    /**
     *
     */
    private static final long serialVersionUID = 2368600068029964377L;

    public TSEMPWeapon() {
        super();
        flags = flags.or(F_TSEMP).or(F_DIRECT_FIRE);
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        maxRange = RANGE_MED;
        heat = 10;
        explosive = true;
        explosionDamage = 10;
        availRating = new int[]{RATING_X,RATING_X,RATING_X,RATING_E};
        techRating = RATING_E;

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
        return new TSEMPHandler(toHit, waa, game, server);
    }    

}
