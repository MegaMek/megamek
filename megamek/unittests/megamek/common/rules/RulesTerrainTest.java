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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.core.CoreRulesTerrain;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.rules.totalwarfare.TWRulesTerrain;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.QuadVee;
import megamek.common.units.SupportTank;
import megamek.common.units.Tank;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.AfterEach;
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
    private final RulesManager previousRules = Game.rulesManager;

    @AfterEach
    void restoreRules() {
        Game.rulesManager = previousRules;
    }

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
        assertEquals(0, core.getRoadElevationCostDifference(vehicleAsset, vehicleAsset.getMovementMode(),
                    true, 2),
                "Core p.142: an Asset's road level change still costs 2 MP per level.");
        QuadVee quadVee = new QuadVee();
        assertEquals(1, core.getRoadElevationCostDifference(quadVee, EntityMovementMode.QUAD, true, 1));
        // The next plotted step can use vehicle mode before the unit completes its conversion.
        assertEquals(1, core.getRoadElevationCostDifference(quadVee, EntityMovementMode.TRACKED, true, 2));
        assertEquals(0, totalWarfare.getRoadElevationCostDifference(mek, mek.getMovementMode(), true, 2),
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
          "TRACKED, pavement:1, pavement:1, 1, 2",
          "TRACKED, pavement:1, road:1:0, 1, 2",
          "TRACKED, road:1:0, pavement:1, 1, 2"
    })
    void ordinaryTankRoadElevationUnderCore(EntityMovementMode mode, String src, String dest, int level,
          int expectedMP) {
        assertEquals(expectedMP, stepCost(new CoreRulesManager(), new Tank(), mode, src, dest, level));
    }

    @ParameterizedTest
    @CsvSource({
          "road:1:0, road:1:0, 1, 1",
          "road:1:0, pavement:1, -1, 1",
          "pavement:1, road:1:0, 1, 1",
          "road:3:0, road:4:0;woods:1, 1, 2",
          "road:1:0, woods:1, 1, 3",
          "woods:1, road:1:0, 1, 2"
    })
    void coreMekPaysRoadCostAcrossRoadOrPavementHexes(String src, String dest, int level, int expectedMP) {
        assertEquals(expectedMP, stepCost(new CoreRulesManager(), new BipedMek(), EntityMovementMode.BIPED,
              src, dest, level));
    }

    @ParameterizedTest
    @CsvSource({ "VEHICLE, TRACKED, 2", "CONV_INFANTRY, INF_LEG, -2" })
    void coreGroundAssetCanChangeTwoLevelsBetweenRoadAndPavement(BFSAssetType type, EntityMovementMode mode,
          int level) {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setAssetType(type);
        assertEquals(5, stepCost(new CoreRulesManager(), asset, mode, "pavement:1", "road:1:0", level));
    }

    @ParameterizedTest
    @CsvSource({
          "road:1:0, road:1:0", "pavement:1, road:1:0",
          "road:1:0, pavement:1", "pavement:1, pavement:1"
    })
    void bothRulesetsAllowWheeledMovementThroughHeavyWoodsOnPavedSteps(String src, String dest) {
        for (RulesManager rules : new RulesManager[] { new CoreRulesManager(), new TWRulesManager() }) {
            assertEquals(1, stepCost(rules, new Tank(), EntityMovementMode.WHEELED, src, dest + ";woods:2", 0));
        }
    }

    @ParameterizedTest
    @CsvSource({
          "HOVER, road:3:0, road:3:0, true", "TRACKED, road:3:0, road:3:0, false",
          "HOVER, road:4:0, road:4:0, true", "TRACKED, road:4:0, road:4:0, true",
          "WHEELED, road:4:0, road:4:0, false", "HOVER, road:3:0, road:4:0, false",
          "WHEELED, pavement:1, road:1:0, true"
    })
    void roadBonusDependsOnSurfaceAndMotiveType(EntityMovementMode mode, String src, String dest, boolean bonus) {
        for (RulesManager rules : new RulesManager[] { new CoreRulesManager(), new TWRulesManager() }) {
            MovePath path = roadPath(rules, new Tank(), mode, src, dest, 0,
                  MoveStepType.FORWARDS, MoveStepType.TURN_LEFT);
            assertTrue(path.isMoveLegal());
            assertEquals(bonus, path.getLastStep().isOnlyPavementOrRoad());
        }
    }

    @ParameterizedTest
    @CsvSource({ "road:1:0", "pavement:1" })
    void oneRoadHexDoesNotLetAWheeledVehicleEnterOrLeaveHeavyWoodsOffRoad(String surface) {
        for (RulesManager rules : new RulesManager[] { new CoreRulesManager(), new TWRulesManager() }) {
            String woods = surface + ";woods:2";
            assertFalse(roadPath(rules, new Tank(), EntityMovementMode.WHEELED, "", woods, 0,
                  MoveStepType.FORWARDS).isMoveLegal());
            assertFalse(roadPath(rules, new Tank(), EntityMovementMode.WHEELED, woods, "", 0,
                  MoveStepType.FORWARDS).isMoveLegal());
        }
    }

    @Test
    void pavedElevationDiscountIncludesDetailedGroundUnitsInCore() {
        for (RulesManager rules : new RulesManager[] { new CoreRulesManager(), new TWRulesManager() }) {
            int discount = rules instanceof CoreRulesManager ? 1 : 0;
            assertEquals(2 - discount, stepCost(rules, new ProtoMek(), EntityMovementMode.BIPED,
                  "pavement:1", "road:1:0", 1));
            assertEquals(3 - discount, stepCost(rules, new ConvInfantry(), EntityMovementMode.INF_LEG,
                  "pavement:1", "road:1:0", 1));
            assertEquals(3 - discount, stepCost(rules, new BattleArmor(), EntityMovementMode.INF_LEG,
                  "road:1:0", "pavement:1", 1));
        }
    }

    @ParameterizedTest
    @CsvSource({ "WHEELED, 2", "TRACKED, 2", "HOVER, 1" })
    void mudOnAnUnpavedRoadStillAffectsGroundVehicles(EntityMovementMode mode, int expected) {
        for (RulesManager rules : new RulesManager[] { new CoreRulesManager(), new TWRulesManager() }) {
            assertEquals(expected, stepCost(rules, new Tank(), mode, "road:3:0", "road:3:0;mud:1", 0));
        }
    }

    @Test
    void wheeledSupportVehiclePaysExtraOffRoad() {
        for (RulesManager rules : new RulesManager[] { new CoreRulesManager(), new TWRulesManager() }) {
            assertEquals(2, stepCost(rules, new SupportTank(), EntityMovementMode.WHEELED, "", "", 0));
            assertEquals(1, stepCost(rules, new SupportTank(), EntityMovementMode.WHEELED, "", "pavement:1", 0));
            assertEquals(1, stepCost(rules, new SupportTank(), EntityMovementMode.WHEELED,
                  "road:3:0", "road:3:0", 0));
        }
    }

    @Test
    void gravelBonusFollowsTheRoadBeneathABridge() {
        for (RulesManager rules : new RulesManager[] { new CoreRulesManager(), new TWRulesManager() }) {
            MovePath path = roadPath(rules, new Tank(), EntityMovementMode.TRACKED, "road:4:0",
                  "road:4:0;bridge:2:0;bridge_elev:3;bridge_cf:40", 0,
                  MoveStepType.FORWARDS, MoveStepType.TURN_LEFT);
            assertTrue(path.isMoveLegal());
            assertTrue(path.getLastStep().isOnlyPavementOrRoad());
            assertFalse(path.getLastStep().isPavementStep());
        }
    }

    @Test
    void optionalMechanizedInfantryBonusIsForPavement() {
        getGame().getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_INF_PAVE_BONUS).setValue(true);
        for (RulesManager rules : new RulesManager[] { new CoreRulesManager(), new TWRulesManager() }) {
            for (EntityMovementMode mode : new EntityMovementMode[] {
                  EntityMovementMode.TRACKED, EntityMovementMode.HOVER }) {
                MovePath gravel = roadPath(rules, new ConvInfantry(), mode, "road:4:0", "road:4:0", 0,
                      MoveStepType.FORWARDS);
                assertTrue(gravel.isMoveLegal());
                assertFalse(gravel.getLastStep().isOnlyPavementOrRoad());
                MovePath paved = roadPath(rules, new ConvInfantry(), mode, "pavement:1", "road:1:0", 0,
                      MoveStepType.FORWARDS);
                assertTrue(paved.isMoveLegal());
                assertTrue(paved.getLastStep().isOnlyPavementOrRoad());
                assertTrue(paved.getEntity().isEligibleForPavementOrRoadBonus());
            }
        }
    }

    @Test
    void totalWarfareMekPaysOnlyTheNormalElevationCostBetweenDisconnectedRoads() {
        assertEquals(2, stepCost(new TWRulesManager(), new BipedMek(), EntityMovementMode.BIPED,
              "road:1:0", "road:1:0;woods:1", 1));
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

    @Test
    void scubaInfantryWalkingOnLandUsesTheFootInfantryRoadCost() {
        assertEquals(2, stepCost(new CoreRulesManager(), new ConvInfantry(), EntityMovementMode.INF_UMU,
              "road:1", "pavement:1", 1));
    }

    @Test
    void aMekMayStepDownOneRoofLevelForOneMpUnderCore() {
        Game.rulesManager = new CoreRulesManager();
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 2
              hex 0101 0 "building:2:8;bldg_cf:80;bldg_elev:2" ""
              hex 0102 0 "building:2:1;bldg_cf:80;bldg_elev:1" ""
              end"""));
        MovePath path = getMovePathFor(new BipedMek(), 2, EntityMovementMode.BIPED,
              MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());
        assertEquals(1, path.getFinalElevation());
        assertEquals(1, path.getMpUsed());
    }

    private int stepCost(RulesManager rules, Entity unit, EntityMovementMode mode, String src, String dest, int level) {
        MovePath path = roadPath(rules, unit, mode, src, dest, level, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());
        return path.getMpUsed();
    }

    private MovePath roadPath(RulesManager rules, Entity unit, EntityMovementMode mode, String src, String dest,
          int level, MoveStepType... steps) {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 2
              hex 0101 0 "%s" ""
              hex 0102 %d "%s" ""
              end""".formatted(src, level, dest)));
        Game.rulesManager = rules;
        return getMovePathFor(unit, 0, mode, steps);
    }
}
