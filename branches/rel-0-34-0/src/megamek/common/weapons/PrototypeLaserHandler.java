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

import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class PrototypeLaserHandler extends EnergyWeaponHandler {

    /**
     * 
     */
    private static final long serialVersionUID = 6832340682515730916L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public PrototypeLaserHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#addHeat()
     */
    protected void addHeat() {
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
            int heat = wtype.getHeat();
            if ( game.getOptions().booleanOption("tacops_energy_weapons") ){
                heat = Compute.dialDownHeat(weapon, wtype,ae.getPosition().distance(target.getPosition()));
            }

            ae.heatBuildup += heat;
            ae.heatBuildup += Compute.d6();
        }
    }

}
