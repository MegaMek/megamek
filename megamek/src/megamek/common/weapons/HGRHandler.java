/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.gameManager.*;

import java.util.Vector;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public class HGRHandler extends GRHandler {
    private static final long serialVersionUID = -6599352761593455842L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public HGRHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (doAmmoFeedProblemCheck(vPhaseReport)) {
            return true;
        }
        
        if ((ae.mpUsed > 0) && (ae instanceof Mech) && ae.canFall()
            // Only check up to assault class, superheavies do not roll.
            && ae.getWeightClass() <= EntityWeightClass.WEIGHT_ASSAULT) {
            // Modifier is weight-based.
            int nMod;
            if (ae.getWeightClass() <= EntityWeightClass.WEIGHT_LIGHT) {
                nMod = 2;
            } else if (ae.getWeightClass() <= EntityWeightClass.WEIGHT_MEDIUM) {
                nMod = 1;
            } else if (ae.getWeightClass() <= EntityWeightClass.WEIGHT_HEAVY) {
                nMod = 0;
            } else {
                nMod = -1;
            }
            PilotingRollData psr = new PilotingRollData(ae.getId(), nMod,
                    "fired HeavyGauss unbraced", false);
            game.addPSR(psr);
        }
        return false;
    }
}
