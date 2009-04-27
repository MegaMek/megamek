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

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.VTOL;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jason Tighe
 */
public class ACFlakHandler extends AmmoWeaponHandler {

    /**
     * 
     */
    private static final long serialVersionUID = -7814754695629391969L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public ACFlakHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();
        
        if ( ((target instanceof VTOL) && !(target instanceof Aero) && !(target instanceof Infantry)) 
                || target instanceof BattleArmor ) {
            toReturn /= 2;
        }
        
        // during a swarm, all damage gets applied as one block to one
        // location
        if (ae instanceof BattleArmor && weapon.getLocation() == BattleArmor.LOC_SQUAD && (ae.getSwarmTargetId() == target.getTargetId())) {
            toReturn *= ((BattleArmor) ae).getShootingStrength();
        }

        if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }
        
        if (game.getOptions().booleanOption("tacops_range") && nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG]) {
            toReturn = (int) Math.floor(toReturn * .75);
        }

        return (int) toReturn;
    }
    
}
