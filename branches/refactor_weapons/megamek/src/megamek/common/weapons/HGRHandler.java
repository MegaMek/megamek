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

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;


/**
 * @author Andrew Hunter
 *
 */
public class HGRHandler extends AmmoWeaponHandler {

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public HGRHandler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
        super(t, w, g, s);
    }
    

    /* (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#doChecks()
     */
    protected void doChecks() {
        if (ae.mpUsed > 0) {
            // the mod is weight-based
            int nMod;
            if (ae.getWeight() <= Mech.WEIGHT_LIGHT) {
              nMod = 2;
            }
            else if (ae.getWeight() <= Mech.WEIGHT_MEDIUM) {
              nMod = 1;
            }
            else if (ae.getWeight() <= Mech.WEIGHT_HEAVY) {
              nMod = 0;
            }
            else {
              nMod = -1;
            }
            PilotingRollData psr = new PilotingRollData(ae.getId(), nMod,
                                                        "fired HeavyGauss unbraced");
            psr.setCumulative(false);
            game.addPSR(psr);
          }
    }
    
    /* (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        int nRange = ae.getPosition().distance(target.getPosition());
        if (nRange <= wtype.getShortRange()) {
            return 25;
        } else if (nRange <= wtype.getMediumRange()) {
            return 20;
        } else {
            return 10;
        }
    }
}
