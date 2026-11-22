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

import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.rules.core.CoreRulesTerrain;
import megamek.common.rules.totalwarfare.TWRulesTerrain;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.QuadVee;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for terrain-specific rule differences between Core and Total Warfare rule sets.
 *
 * The tests aim to mirror the documented style used in the planetary conditions tests: small helper
 * constructors for test fixtures, clear DisplayName annotations and explanatory assertion messages.
 */
@DisplayName("RulesTerrain rules variants")
class RulesTerrainTest {

    @Test
    @DisplayName("core and total warfare road and elevation logic differ by rule set")
    void coreAndTotalWarfareTerrainLogicDiffersByRuleSet() {
        CoreRulesTerrain core = new CoreRulesTerrain();
        TWRulesTerrain totalWarfare = new TWRulesTerrain();

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesTerrain.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesTerrain.class, totalWarfare, "Total warfare rules should extend the base rules type.");

        Mek mek = new BipedMek();
        BattlefieldSupportAsset vehicleAsset = new BattlefieldSupportAsset();
        vehicleAsset.setAssetType(BFSAssetType.VEHICLE);

        assertEquals(1, core.getRoadElevationCostDifference(mek, mek.getMovementMode(), true, 2),
                "Core p.53: a level change between road hexes costs 1 MP less.");
        assertEquals(0, core.getRoadElevationCostDifference(vehicleAsset, EntityMovementMode.TRACKED, true, 2),
                "Core p.142: an Asset's road level change still costs 2 MP per level.");
        assertEquals(0, core.getRoadElevationCostDifference(new Tank(), EntityMovementMode.TRACKED, true, 2));
        QuadVee quadVee = new QuadVee();
        assertEquals(1, core.getRoadElevationCostDifference(quadVee, EntityMovementMode.QUAD, true, 1));
        // The next plotted step can use vehicle mode before the unit completes its conversion.
        assertEquals(0, core.getRoadElevationCostDifference(quadVee, EntityMovementMode.TRACKED, true, 2));
        assertEquals(0, totalWarfare.getRoadElevationCostDifference(mek, mek.getMovementMode(), true, 2),
                "Total Warfare does not change road elevation cost.");
        assertEquals(2, core.getMaxElevationChangeAllowed(mek, true, 2),
                "Core p.53 gives Meks no extra level on roads.");
        assertEquals(2, core.getMaxElevationChangeAllowed(vehicleAsset, true, 1),
                "Core p.142: vehicle and infantry Assets change up to two levels between road hexes.");
        assertEquals(3, totalWarfare.getMaxElevationChangeAllowed(vehicleAsset, true, 3),
                "Total Warfare keeps the normal elevation cap even on roads.");
    }
}
