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
 */
package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.Hex;
import megamek.common.rules.core.CoreRulesTerrain;
import megamek.common.rules.totalwarfare.TWRulesTerrain;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RulesTerrain rules variants")
class RulesTerrainTest {

    @Test
    @DisplayName("core and total warfare road and elevation logic differ by rule set")
    void coreAndTotalWarfareTerrainLogicDiffersByRuleSet() {
        CoreRulesTerrain core = new CoreRulesTerrain();
        TWRulesTerrain totalWarfare = new TWRulesTerrain();
        Hex roadA = new Hex(0, new Terrain[] {new Terrain(Terrains.ROAD, 1)}, null);
        Hex roadB = new Hex(0, new Terrain[] {new Terrain(Terrains.ROAD, 1)}, null);

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesTerrain.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesTerrain.class, totalWarfare, "Total warfare rules should extend the base rules type.");

        assertEquals(-1, core.getRoadElevationCostDifference(roadA, roadB, 2),
                "Core road movement should reduce elevation cost by one when traversing roads upward.");
        assertEquals(0, totalWarfare.getRoadElevationCostDifference(roadA, roadB, 2),
                "Total Warfare does not change road elevation cost.");
        assertEquals(4, core.getMaxElevationChangeAllowed(roadA, roadB, 3),
                "Core roads allow one more elevation change than the normal max.");
        assertEquals(3, totalWarfare.getMaxElevationChangeAllowed(roadA, roadB, 3),
                "Total Warfare keeps the normal elevation cap even on roads.");
    }
}
