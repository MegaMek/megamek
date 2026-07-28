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

import megamek.common.CriticalSlot;
import megamek.common.LosEffects;
import megamek.common.compute.Compute;
import megamek.common.rules.core.CoreRulesTarget;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;

public class TWRulesTarget extends CoreRulesTarget {

    /**
     * Check if the target is large and if there is a modifier.
     *
     * @param weightclass the weight class of the target
     * @param markedLarge true if the target is marked as large
     * @return the large target modifier
     */
    @Override
    public int largeTargetModifier(int weightclass, boolean markedLarge) {
        if (weightclass == EntityWeightClass.WEIGHT_SUPER_HEAVY || weightclass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
            return -1;
        }
        return 0;
    }

    /**
     * Do we hit the aimed location?
     * Aimed shots hit if you roll 6-8
     *
     * @return true if the aimed location is hit
     */
    @Override
    public boolean checkAimedLocation() {
        int roll = Compute.d6(2);

        if ((5 < roll) && (roll < 9)) {
            return true;
        }
        return false;
    }

    /**
     * What is the secondary arc modifier? 2
     *
     * @return the secondary arc modifier
     */
    @Override
    public int getSecondaryArcModifier(){
        return 2;
    }

    /**
     * Can you shoot with one arm while prone
     * Only with TacOps Prone firing enabled
     *
     * @param toProneFire true if checking prone fire capability
     * @return true if you can shoot with one arm while prone
     */
    @Override
    public boolean proneFireWithOneArm(final boolean toProneFire) {
        return toProneFire;
    }

    /**
     * What is the arm actuator hit mod for shooting.
     * Both arm actuators increase the to hit for shooting.
     *
     * @param attacker the attacking entity
     * @param location the arm location being used
     * @return the arm actuator hit modifier
     */
    @Override
    public int getArmActuatorHitMod(Entity attacker, int location) {
        int actuatorHits = 0;
        if (attacker.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_ARM, location) > 0) {
            actuatorHits++;
        }
        if (attacker.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_ARM, location) > 0) {
            actuatorHits++;
        }
        return actuatorHits;
    }

    /**
     * Do we reduce smoke? No
     *
     * @param los the line of sight effects
     * @return the BAP smoke reduction amount. Always is 0
     */
    @Override
    public int getBAPSmokeReduction(LosEffects los) {
        return 0;
    }
}
