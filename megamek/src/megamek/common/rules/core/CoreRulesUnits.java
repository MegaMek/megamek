package megamek.common.rules.core;
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
import megamek.common.units.QuadMek;

public class CoreRulesUnits extends RulesUnits {
    /**
     * {@inheritDoc}
     * Mule kicks have no additional modifier Core p.238
     */
    @Override
    public int getMuleKickModifier() { return 0; }

    /**
     * {@inheritDoc}
     * Is it immobile due to leg destruction? Core p.237 (tripod), p.239 (quad), p.90
     */
    @Override
    public boolean getDoesLegDestructionCauseImmobile(Mek mek) {
        int legsDestroyed = 0;
        for (int i = 0; i < mek.locations(); i++) {
            if (mek.locationIsLeg(i)) {
                if (mek.isLocationBad(i)) {
                    legsDestroyed++;
                }
            }
        }
        if (legsDestroyed >= 2 && !(mek instanceof QuadMek)) {
            return true;
        } else if (legsDestroyed == 4 && (mek instanceof QuadMek)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * reduce a quad's walk MP for legs destroyed, hip hits, and actuator hits. Core p.90, 238
     */
    @Override
    public int reduceQuadWalkMP(int mp, int legsDestroyed, int hipHits, int actuatorHits,
          boolean bTOLegDamage) {
        if (legsDestroyed > 0) {
            if (legsDestroyed == 1) {
                mp--;
            } else if (legsDestroyed == 2) {
                mp = mp - 2;
            } else if (legsDestroyed == 3) {
                mp = 1;
            } else if (legsDestroyed == 4) {
                mp = 0;
            }
        }
        mp -= hipHits;
        mp -= actuatorHits;

        if (mp > 0) {
            return mp;
        }
        if (legsDestroyed < 4 && mp <= 0) {
            return 1;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * Quads modify PSR rolls for legs as per Core p.238
     */
    @Override
    public void quadPilotModForLegsDestroyed(int destroyedLegs, PilotingRollData roll) {
        switch (destroyedLegs) {
            case 1:
                roll.addModifier(1, "1 leg destroyed");
                break;
            case 2:
                roll.addModifier(2, "2 legs destroyed");
                break;
            case 3:
                roll.addModifier(Game.rulesManager.getRulesPSR().getLegDestroyedModifier(), "3 legs destroyed");
                break;
        }
    }

    /**
     * {@inheritDoc}
     * Reduce MP for a mek with hip hits. Core p.99
     */
    @Override
    public int getMekMPReduction(int hipHits, boolean bTOLegDamage, int mp) {
        mp -= hipHits;
        return mp;
    }

    /**
     * {@inheritDoc}
     * MP cannot be reduced below 1 by actuator damage. only by leg destruction Core p.99
     */
    @Override
    public int getMinimumMP(int mp) {
        return 1;
    }

    /**
     * {@inheritDoc}
     * Does it have what counts as bad legs? 1 for bipeds/tripods, 3 for quads
     * Gets the value from the entity
     */
    @Override
    public boolean hasBadLegs(Entity entity) {
        return entity.hasBadLegs();
    }

    /**
     * {@inheritDoc}
     * Core allows Torso twisting in physical if you haven't already.
     * Returns true.
     */
    @Override
    public boolean getPhysicalTwistEnabled() {
        return true;
    }
}
