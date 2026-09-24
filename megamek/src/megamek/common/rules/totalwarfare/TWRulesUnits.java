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
import megamek.common.rolls.PilotingRollData;
import megamek.common.rules.RulesUnits;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

public class TWRulesUnits extends RulesUnits {
    /**
     * Mule kicks are +1 to hit
     *
     * @return the mule kick modifier
     */
    @Override
    public int getMuleKickModifier() { return 1; }
    
    /**
     * Does removing legs cause it to be immobile? No
     *
     * @param mek the MEK to check
     * @return always returns false
     */
    @Override
    public boolean getDoesLegDestructionCauseImmobile(Mek mek) {
        return false;
    }

    /**
     * Reduce a quad's walk MP for legs destroyed, hip hits, and actuator hits.
     *
     * @param mp the base movement points
     * @param legsDestroyed the number of legs destroyed
     * @param hipHits the number of hip hits
     * @param actuatorHits the number of actuator hits
     * @param bTOLegDamage true if using tactical operations leg damage rules
     * @return the reduced movement points
     */
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

    /**
     * What modifiers to we add for legs destroyed on a quad.
     *
     * @param destroyedLegs the number of destroyed legs
     * @param roll the piloting roll data to modify
     */
    @Override
    public void quadPilotModForLegsDestroyed(int destroyedLegs, PilotingRollData roll) {
        if (destroyedLegs == 2) {
            roll.addModifier(Game.rulesManager.getRulesPSR().getLegDestroyedModifier(), "2 legs destroyed");
        }
    }

    /**
     * Reduce MP for a mek with hip hits.
     *
     * @param hipHits the number of hip hits
     * @param bTOLegDamage true if using tactical operations leg damage rules
     * @param mp the base movement points
     * @return the reduced movement points
     */
    @Override
    public int getMekMPReduction(int hipHits, boolean bTOLegDamage, int mp) {
        if (bTOLegDamage) {
            mp = mp - 2 * hipHits;
        } else {
            mp = (hipHits == 1) ? (int) Math.ceil(mp / 2.0) : 0;
        }
        return mp;
    }

    /**
     * Is there a limit to how much we can reduce MP? 0
     *
     * @param mp the movement points
     * @return returns the input parameter with no change
     */
    @Override
    public int getMinimumMP(int mp) {
        return mp;
    }

    /**
     * Bad legs check always returns false
     *
     * @param entity the entity to check
     * @return always returns false
     */
    @Override
    public boolean hasBadLegs(Entity entity) {
        return false;
    }
}
