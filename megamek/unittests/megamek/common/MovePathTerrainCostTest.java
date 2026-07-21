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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.pathfinder.MovementType;
import org.junit.jupiter.api.Test;

class MovePathTerrainCostTest {

    @Test
    void wadingCostIsZeroForAFordAndDryLand() {
        // Depth 0 (a ford) and hexes with no water carry no wading cost, so the A* treats a ford like clear
        // terrain instead of pacing at the bank (issue #7627).
        assertEquals(0, MovePathTerrainCost.wadingCost(MovementType.Walker, 0, 6, 4));
        assertEquals(0, MovePathTerrainCost.wadingCost(MovementType.Walker, Integer.MIN_VALUE, 6, 4));
    }

    @Test
    void wadingCostScalesWithDepthAndForfeitedRunningSpeed() {
        // Depth 1: forfeited running (run 6 - walk 4 = 2) + depth penalty 1 = 3.
        assertEquals(3, MovePathTerrainCost.wadingCost(MovementType.Walker, 1, 6, 4));
        // Depth 2: forfeited running 2 + depth penalty 3 = 5.
        assertEquals(5, MovePathTerrainCost.wadingCost(MovementType.Walker, 2, 6, 4));
        // Depth 3 uses the same Depth 2+ penalty; forfeited running (8 - 5 = 3) + 3 = 6.
        assertEquals(6, MovePathTerrainCost.wadingCost(MovementType.Walker, 3, 8, 5));
    }

    @Test
    void wadingCostWithNoForfeitedSpeedIsJustTheDepthPenalty() {
        assertEquals(1, MovePathTerrainCost.wadingCost(MovementType.Walker, 1, 4, 4));
        assertEquals(3, MovePathTerrainCost.wadingCost(MovementType.Walker, 2, 4, 4));
    }

    @Test
    void amphibiousGroundUnitsAreChargedForWading() {
        assertEquals(3, MovePathTerrainCost.wadingCost(MovementType.WheeledAmphibious, 1, 6, 4));
        assertEquals(3, MovePathTerrainCost.wadingCost(MovementType.TrackedAmphibious, 1, 6, 4));
    }

    @Test
    void unitsNotSlowedByWadingAreNotCharged() {
        // Hover units skim the surface, so they take no wading cost regardless of depth.
        assertEquals(0, MovePathTerrainCost.wadingCost(MovementType.Hover, 2, 6, 4));
    }
}
