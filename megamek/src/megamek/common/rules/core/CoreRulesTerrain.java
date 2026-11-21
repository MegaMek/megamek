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
import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.rules.RulesTerrain;
import megamek.common.units.Entity;
import megamek.common.units.Terrains;

public class CoreRulesTerrain extends RulesTerrain {

    /**
     * {@inheritDoc}
     * A level change between two road hexes costs 1 MP less (Core p.53). Assets get no discount: their road level
     * change "still costs 2 MP per level change" (Core p.142).
     */
    @Override
    public int getRoadElevationCostDifference(Entity entity, Hex srcHex, Hex destHex, int deltaElevation) {
        if (!entity.isBattlefieldSupportAsset() && isRoadToRoad(srcHex, destHex) && (deltaElevation > 0)) {
            return 1;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * Travelling between two road hexes, vehicle and infantry Assets "can make a level change of up to two levels,
     * instead of one" (Core p.142). Meks get no extra level on roads (Core p.53); other units keep Total Warfare.
     */
    @Override
    public int getMaxElevationChangeAllowed(Entity entity, Hex srcHex, Hex destHex, int maxElevationChange) {
        if ((entity instanceof BattlefieldSupportAsset asset) && (asset.getAssetType() != BFSAssetType.EMPLACEMENT)
              && isRoadToRoad(srcHex, destHex) && (maxElevationChange != Entity.UNLIMITED_JUMP_DOWN)) {
            return maxElevationChange + 1;
        }
        return maxElevationChange;
    }

    private static boolean isRoadToRoad(Hex srcHex, Hex destHex) {
        return srcHex.containsTerrain(Terrains.ROAD) && destHex.containsTerrain(Terrains.ROAD);
    }
}
