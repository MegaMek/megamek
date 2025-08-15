/*
  Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons;

import java.util.Vector;

import megamek.common.EntityWeightClass;
import megamek.common.Game;
import megamek.common.Mek;
import megamek.common.PilotingRollData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.totalwarfare.TWGameManager;

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
    public HGRHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
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

        if ((ae.mpUsed > 0) && (ae instanceof Mek) && ae.canFall()
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
