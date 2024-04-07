/*
 * MegaMek - Copyright (C) 2013 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.other;

import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.TSEMPHandler;
import megamek.common.weapons.lasers.EnergyWeapon;
import megamek.server.gameManager.*;

/**
 * Tight-Stream Electro-Magnetic Pulse (TSEMP) weapon. Found in FM:3145 pg 255.
 * 
 * @author arlith
 */
public class TSEMPWeapon extends EnergyWeapon {

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
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, GameManager manager) {
        return new TSEMPHandler(toHit, waa, game, manager);
    }
}
