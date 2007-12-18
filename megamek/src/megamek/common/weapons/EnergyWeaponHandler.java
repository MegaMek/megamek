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

import megamek.common.BattleArmor;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

public class EnergyWeaponHandler extends WeaponHandler {
    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public EnergyWeaponHandler(ToHitData toHit, WeaponAttackAction waa, IGame g,
            Server s) {
        super(toHit, waa, g, s);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        float toReturn = wtype.getDamage();
        // Check for Altered Damage from Energy Weapons (MTR, pg.22)
        int nRange = ae.getPosition().distance(target.getPosition());
        if (game.getOptions().booleanOption("maxtech_altdmg")) {
            if (nRange <= 1) {
                toReturn++;
            } else if (nRange <= wtype.getMediumRange()) {
                // Do Nothing for Short and Medium Range
            } else if (nRange <= wtype.getLongRange()) {
                toReturn--;
            } else if (nRange <= wtype.getExtremeRange()) {
                toReturn = (int)Math.floor(toReturn/2.0);
            }
        }
        if (bGlancing) {
            toReturn = toReturn/=2;
        }
        if (target instanceof Infantry && !(target instanceof BattleArmor)) 
        	toReturn /= 10;
        return Math.round(toReturn);
    }        

}
