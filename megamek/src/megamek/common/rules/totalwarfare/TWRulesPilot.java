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

import megamek.common.Report;
import megamek.common.TargetRollModifier;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesPilot;
import megamek.common.units.Entity;

import java.util.List;
import java.util.Vector;

public class TWRulesPilot extends RulesPilot {

    /**
     * Two pilot hits for an explosion
     *
     * @return the number of pilot hits caused by an explosion
     */
    @Override
    public int getExplosionPilotHits() {
        return 2;
    }

    /**
     * Gyro destroyed seatbelt check is +6
     *
     * @param piloting the piloting skill
     * @return the seatbelt gyro modifier
     */
    @Override
    public int getSeatbeltGyroModifier(int piloting) {
        return piloting + 6;
    }

    /**
     * Do we modify seatbelt by legs destroyed?
     * Yes, increasing difficulties
     *
     * @param piloting the piloting skill
     * @param legsDestroyed the number of legs destroyed
     * @return the seatbelt leg modifier
     */
    @Override
    public int getSeatbeltLegModifier(int piloting, int legsDestroyed) {
        if (legsDestroyed == 2) {
            return piloting + 10;
        }
        if (legsDestroyed >=3) {
            return piloting + legsDestroyed * 5;
        }
        return piloting;
    }

    /**
     * What is the seatbelt check on shutdown.
     * Reactor shutdown is harder on the check
     *
     * @param piloting the piloting skill
     * @return the seatbelt shutdown target number
     */
    @Override
    public int getSeatbeltShutdown(int piloting) {
        return piloting + 3;
    }

    /**
     * {@inheritDoc}
     * In TW this is fall height - 1
     * */
    @Override
    public int getSeatbeltHeightModifier(int fallHeight) { return fallHeight - 1; }

    /**
     * {@inheritDoc}
     */
    @Override
    public PilotingRollData getSeatbeltRoll(Entity entity,
           int fallHeight,
          int piloting,
          List<TargetRollModifier> modifiers,
          PilotingRollData roll) {
        if (modifiers == null) {
            return roll;
        }
        PilotingRollData prd = new PilotingRollData(entity.getId(),
              piloting,
              "Base piloting skill");
        if (!modifiers.isEmpty()) {
            modifiers.forEach(prd::addModifier);
        }
        return prd;        
    }
}
