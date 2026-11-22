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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.Hex;
import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.core.CoreRulesTerrain;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.rules.totalwarfare.TWRulesTerrain;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.QuadVee;
import megamek.common.units.Tank;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for terrain-specific rule differences between Core and Total Warfare rule sets.
 *
 * The tests aim to mirror the documented style used in the planetary conditions tests: small helper
 * constructors for test fixtures, clear DisplayName annotations and explanatory assertion messages.
 */
@DisplayName("RulesTerrain rules variants")
class RulesTerrainTest extends GameBoardTestCase {

    @Test
    @DisplayName("core and total warfare road and elevation logic differ by rule set")
    void coreAndTotalWarfareTerrainLogicDiffersByRuleSet() {
        CoreRulesTerrain core = new CoreRulesTerrain();
        TWRulesTerrain totalWarfare = new TWRulesTerrain();

        assertNotNull(core, "Core rules implementation should be constructible.");
        assertNotNull(totalWarfare, "Total warfare rules implementation should be constructible.");
        assertInstanceOf(RulesTerrain.class, core, "Core rules should extend the base rules type.");
        assertInstanceOf(RulesTerrain.class, totalWarfare, "Total warfare rules should extend the base rules type.");

        Hex src = new Hex(0, "road:1", "");
        Hex dest = new Hex(1, "road:1", "");
        Mek mek = new BipedMek();
        BattlefieldSupportAsset vehicleAsset = new BattlefieldSupportAsset();
        vehicleAsset.setAssetType(BFSAssetType.VEHICLE);

        assertEquals(1, core.getRoadElevationCostDifference(mek, mek.getMovementMode(), src, dest, true, 2),
                "Core p.53: a level change between road hexes costs 1 MP less.");
        assertEquals(0, core.getRoadElevationCostDifference(vehicleAsset, vehicleAsset.getMovementMode(),
                    src, dest, true, 2),
                "Core p.142: an Asset's road level change still costs 2 MP per level.");
        QuadVee quadVee = new QuadVee();
        assertEquals(1, core.getRoadElevationCostDifference(quadVee, EntityMovementMode.QUAD, src, dest, true, 1));
        // The next plotted step can use vehicle mode before the unit completes its conversion.
        assertEquals(0, core.getRoadElevationCostDifference(quadVee, EntityMovementMode.TRACKED, src, dest, true, 2));
        assertEquals(0, totalWarfare.getRoadElevationCostDifference(mek, mek.getMovementMode(), src, dest, true, 2),
                "Total Warfare does not change road elevation cost.");
        assertEquals(2, core.getMaxElevationChangeAllowed(mek, true, 2),
                "Core p.53 gives Meks no extra level on roads.");
        assertEquals(2, core.getMaxElevationChangeAllowed(vehicleAsset, true, 1),
                "Core p.142: vehicle and infantry Assets change up to two levels between road hexes.");
        assertEquals(3, totalWarfare.getMaxElevationChangeAllowed(vehicleAsset, true, 3),
                "Total Warfare keeps the normal elevation cap even on roads.");
    }

    @ParameterizedTest
    @CsvSource({
          "TRACKED, road:1, road:1, 1, 2",
          "TRACKED, road:1, road:1, -1, 2",
          "TRACKED, road:1, road:1, 0, 1",
          "WHEELED, road:1, road:1, 1, 2",
          "HOVER, road:1, road:1, 1, 2",
          "TRACKED, road:1:0, road:1:0, 1, 2",
          "TRACKED, pavement:1, pavement:1, 1, 3",
          "TRACKED, pavement:1, road:1, 1, 3",
          "TRACKED, road:1, pavement:1, 1, 3"
    })
    void ordinaryTankRoadElevationUnderCore(EntityMovementMode mode, String src, String dest, int level,
          int expectedMP) {
        assertEquals(expectedMP, stepCost(new CoreRulesManager(), new Tank(), mode, src, dest, level));
    }

    @Test
    void coreVehicleAssetPaysFullElevationCostOnRoads() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setAssetType(BFSAssetType.VEHICLE);
        assertEquals(3, stepCost(new CoreRulesManager(), asset, EntityMovementMode.TRACKED, "road:1", "road:1", 1));
    }

    @Test
    void totalWarfareTankPaysFullElevationCostOnRoads() {
        assertEquals(3, stepCost(new TWRulesManager(), new Tank(), EntityMovementMode.TRACKED, "road:1", "road:1", 1));
    }

    private int stepCost(RulesManager rules, Entity unit, EntityMovementMode mode, String src, String dest, int level) {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 2
              hex 0101 0 "%s" ""
              hex 0102 %d "%s" ""
              end""".formatted(src, level, dest)));
        RulesManager saved = Game.rulesManager;
        try {
            Game.rulesManager = rules;
            MovePath path = getMovePathFor(unit, 0, mode, MoveStepType.FORWARDS);
            assertTrue(path.isMoveLegal());
            return path.getMpUsed();
        } finally {
            Game.rulesManager = saved;
        }
    }
}
