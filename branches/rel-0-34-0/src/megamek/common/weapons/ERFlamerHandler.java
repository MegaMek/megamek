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
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class ERFlamerHandler extends FlamerHandler {
    /**
     *
     */
    private static final long serialVersionUID = -7348456582587703751L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public ERFlamerHandler(ToHitData toHit, WeaponAttackAction waa, IGame g,
            Server s) {
        super(toHit, waa, g, s);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        int toReturn;
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            if (ae instanceof BattleArmor) {
                toReturn = Compute.d6(3);
            }
            toReturn = Compute.d6(2);
            if ( bDirect ) {
                toReturn += toHit.getMoS()/3;
            }
            // pain shunted infantry get half damage
            if (((Entity) target).getCrew().getOptions().booleanOption("pain_shunt")) {
                toReturn = (int) Math.floor(toReturn / 2.0);
            }
            if (bGlancing) {
                toReturn = (int) Math.floor(toReturn / 2.0);
            }
        } else {
            toReturn = super.calcDamagePerHit();
        }
        return toReturn;
    }
}