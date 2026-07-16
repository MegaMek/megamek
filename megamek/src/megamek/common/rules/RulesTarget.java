package megamek.common.rules;
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

import megamek.common.LosEffects;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

import java.util.ArrayList;

public abstract class RulesTarget {
    // Check if the target is large and if there is a modifier
    public abstract int largeTargetModifier(int weightclass, boolean markedLarge);
    public int largeTargetModifier(int weightclass) { return largeTargetModifier(weightclass, false); };
    public int largeTargetModifier(boolean markedLarge) {return largeTargetModifier(0,markedLarge);};

    // Do we hit the aimed location?
    public abstract boolean checkAimedLocation();

    // What is the secondary arc modifier
    public abstract int getSecondaryArcModifier();

    // Can you shoot with one arm while prone
    public abstract boolean proneFireWithOneArm(boolean toProneFire);

    // What is the arm actuator hit mod for shooting
    public abstract int getArmActuatorHitMod(Entity attacker, int location);

    // Do we reduce smoke?
    public abstract int getBAPSmokeReduction(LosEffects los);
}
