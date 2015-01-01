/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Oct 19, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.BattleArmor;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class SNPPCHandler extends PPCHandler {

    /**
     * 
     */
    private static final long serialVersionUID = -2107847606508556295L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public SNPPCHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        double toReturn;
        int nRange = ae.getPosition().distance(target.getPosition());
        if (nRange <= wtype.getShortRange()) {
            toReturn = 10;
        } else if (nRange <= wtype.getMediumRange()) {
            toReturn = 8;
        } else {
            toReturn = 5;
        }
        if (weapon.hasChargedCapacitor()) {
            toReturn += 5;
        }
        if (target instanceof Infantry && !(target instanceof BattleArmor))
            toReturn /= 10;
        toReturn = Math.ceil(toReturn);
        if (bGlancing)
            toReturn = (int) Math.floor(toReturn / 2.0);
        return (int)toReturn;
    }
}
