package megamek.common.rules.totalwarfare;
/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rules.core.CoreRulesUnits;
import megamek.common.units.Mek;

public class TWRulesUnits extends CoreRulesUnits {
    // Mule kicks have a +1 modifier
    @Override
    public int getMuleKickModifier() { return 1; }
    
    // Leg destruction does not cause immobile
    @Override
    public boolean getDoesLegDestructionCauseImmobile(Mek mek) {
        return false;
    }

    // reduce a quad's walk MP for legs destroyed, hip hits, and actuator hits. Core p.90, 238
    @Override
    public int reduceQuadWalkMP(int mp, int legsDestroyed, int hipHits, int actuatorHits,
          boolean bTOLegDamage) {
        if (legsDestroyed > 0) {
            if (legsDestroyed == 1) {
                mp--;
            } else if (legsDestroyed == 2) {
                mp = 1;
            } else {
                mp = 0;
            }
        }
        if (mp > 0) {
            if (hipHits > 0) {
                if (bTOLegDamage) {
                    mp = mp - (2 * hipHits);
                } else {
                    for (int i = 0; i < hipHits; i++) {
                        mp = (int) Math.ceil(mp / 2.0);
                    }
                }
            }
            mp -= actuatorHits;
        }
        if (mp >= 0) {
            return mp;
        }
        return 0;
    }

    // Quads modify PSR rolls for legs
    @Override
    public void quadPilotModForLegsDestroyed(int destroyedLegs, PilotingRollData roll) {
        if (destroyedLegs == 2) {
            roll.addModifier(Game.rulesManager.getRulesPSR().getLegDestroyedModifier(), "2 legs destroyed");
        }
    }

    // Reduce MP for a mek with hip hits.
    @Override
    public int getMekMPReduction(int hipHits, boolean bTOLegDamage, int mp) {
        if (bTOLegDamage) {
            mp = mp - 2 * hipHits;
        } else {
            mp = (hipHits == 1) ? (int) Math.ceil(mp / 2.0) : 0;
        }
        return mp;
    }

    // MP can be reduced to 0 by actuators
    @Override
    public int getMinimumMP(int mp) {
        return mp;
    }
}
