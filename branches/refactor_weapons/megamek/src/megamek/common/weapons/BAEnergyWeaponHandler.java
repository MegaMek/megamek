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

import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

public class BAEnergyWeaponHandler extends BAWeaponHandler {
    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public BAEnergyWeaponHandler(ToHitData toHit, WeaponAttackAction waa, IGame g,
            Server s) {
        super(toHit, waa, g, s);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        // Check for Altered Damage from Energy Weapons (MTR, pg.22)
        int nDamPerHit = wtype.getRackSize();
        int nRange = ae.getPosition().distance(target.getPosition());
        if (game.getOptions().booleanOption("maxtech_altdmg")) {
            if (nRange <= 1) {
                nDamPerHit++;
            } else if (nRange <= wtype.getMediumRange()) {
                // Do Nothing for Short and Medium Range
            } else if (nRange <= wtype.getLongRange()) {
                nDamPerHit--;
            } else if (nRange <= wtype.getExtremeRange()) {
                nDamPerHit = (int)Math.floor(nDamPerHit/2.0);
            }
        }
        if (bGlancing) {
            return (int)Math.floor(nDamPerHit/2.0);
        } else {
            return nDamPerHit;
        }
    }
}
