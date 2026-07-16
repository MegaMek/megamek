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

import megamek.common.CriticalSlot;
import megamek.common.LosEffects;
import megamek.common.TargetRollModifier;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.rules.RulesTarget;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;

import java.util.ArrayList;
import java.util.List;

public class CoreRulesTarget extends RulesTarget {
    /**
     * Large targets get a -1 modifier to hit them. Superheavy meks are large targets Core rules page 64, 240
     */
    public int largeTargetModifier(int weightclass, boolean markedLarge) {
        if (weightclass == EntityWeightClass.WEIGHT_SUPER_HEAVY
              || weightclass == EntityWeightClass.WEIGHT_LARGE_SUPPORT
              || markedLarge) {
            return -1;
        }
        return 0;
    }

    // Aimed shots hit on d6 4+. Core p.70
    public boolean checkAimedLocation() {
        int roll = Compute.d6(1);
        if (roll >= 4) {
            return true;
        }
        return false;
    }

    // Secondary arcs are +1. Core p.64
    public int getSecondaryArcModifier() {
        return 1;
    }

    // Can shoot with one arm while prone. Core p.67
    public boolean proneFireWithOneArm(final boolean toProneFire) {
        return true;
    }

    // Only upper arm actuators increase the to hit for shooting. Core p.97
    public int getArmActuatorHitMod(Entity attacker, int location) {
        if (attacker.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_ARM, location) > 0) {
            return 1;
        }
        return 0;
    }
    
    // BAP reduces smoke within its range. It is blocked by ECM (Handled prior to this call) Core p.197
    public int getBAPSmokeReduction(Entity attacker, Targetable target, int totalSmoke) {
        LosEffects los;
        int smokeEffect = 0;
        int lowestModifier = 0;
        boolean setLowest = false;
        ArrayList<Coords> probeCoords = coordsOnPath(attacker.getPosition(), target.getPosition(),
              attacker.getBAPRange());
        if (probeCoords.size() > 0) {
            for (Coords position : probeCoords) {
                int totalModifier = 0;
                los = LosEffects.calculateLOS(attacker.getGame(), attacker, target, attacker.getPosition(), position,
                      false);
                ToHitData getTotalModifiers = los.losModifiers(attacker.getGame());
                int tempSmokeEffect = (los.getHeavySmoke()*2) + los.getLightSmoke();
                List<TargetRollModifier> targetModifiers = getTotalModifiers.getModifiers();
                for (TargetRollModifier modifier : targetModifiers) {
                    totalModifier += modifier.value();
                }
                if (totalModifier < lowestModifier || !setLowest) {
                    lowestModifier = totalModifier;
                    smokeEffect = tempSmokeEffect;
                    setLowest = true;
                }
            }
        }
        return smokeEffect;
    }
}
