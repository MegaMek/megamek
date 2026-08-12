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

import megamek.common.Hex;
import megamek.common.rules.RulesTerrain;
import megamek.common.units.Entity;
import megamek.common.units.Terrains;

public class CoreRulesTerrain extends RulesTerrain {
    
    /**
     * {@inheritDoc}
     * Road to road elevation change is -1 MP
     */
    @Override
    public int getRoadElevationCostDifference(Hex srcHex, Hex destHex, int deltaElevation) {
        if (srcHex.containsTerrain(Terrains.ROAD) && destHex.containsTerrain(Terrains.ROAD) && deltaElevation > 0) {
            return -1;
        }        
        return 0;
    }

    /**
     * {@inheritDoc}
     * Using roads can increase the max elevation change by 1. Core p.53
     */
    @Override
    public int getMaxElevationChangeAllowed(Hex srcHex, Hex destHex, int maxElevationChange) {
        if (srcHex.containsTerrain(Terrains.ROAD) && destHex.containsTerrain(Terrains.ROAD)) {
            return (maxElevationChange != Entity.UNLIMITED_JUMP_DOWN) ? maxElevationChange + 1 : maxElevationChange;
        }
        return maxElevationChange;
    }
}
