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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

import java.util.UUID;

import megamek.common.GameBoardTestCase;
import megamek.common.Hex;
import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BFSSpecial;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.pathfinder.BoardEdgePathFinder;
import megamek.common.planetaryConditions.Fog;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RoadHazards;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;
import megamek.common.units.VTOL;
import megamek.server.ServerHelper;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RoadHazardsTest extends GameBoardTestCase {
    private final Coords start = new Coords(0, 0);
    private final Coords end = new Coords(0, 1);

    private MovePath drive(Entity unit, EntityMovementMode mode, String from, String into) {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 2
              hex 0101 0 "%s" ""
              hex 0102 0 "%s" ""
              end""".formatted(from, into)));
        return getMovePathFor(unit, 0, mode, MoveStepType.FORWARDS);
    }

    @ParameterizedTest
    @CsvSource({ "road:3", "road:4" })
    void aRoadThroughRubbleDoesNotCauseAPilotingRoll(String surface) {
        BipedMek mek = new BipedMek();
        MovePath path = drive(mek, EntityMovementMode.BIPED, surface, surface + ";rubble:1");
        MoveStep step = path.getLastStep();
        assertTrue(path.isMoveLegal());
        assertEquals(1, path.getMpUsed());
        assertEquals(TargetRoll.CHECK_FALSE, mek.checkRubbleMove(step, EntityMovementType.MOVE_WALK,
              getGame().getBoard().getHex(end), start, end, true, step.isRoadStep()).getValue());
        assertFalse(Compute.isPilotingSkillNeeded(getGame(), mek.getId(), start, end, EntityMovementType.MOVE_WALK,
              false, false, 0, 0, step));
    }

    @ParameterizedTest
    @CsvSource({ "road:3;swamp:2, false", "road:3;swamp:2;mud:1, true", "road:4;mud:1, true" })
    void mudOnAnUnpavedRoadStillRequiresATrackedVehicleToCheckForBogging(String destination, boolean bogs) {
        Tank tank = new Tank();
        MovePath path = drive(tank, EntityMovementMode.TRACKED, "road:3", destination);
        MoveStep step = path.getLastStep();
        assertTrue(path.isMoveLegal());
        Hex hex = getGame().getBoard().getHex(end);
        int target = tank.checkBogDown(step, EntityMovementType.MOVE_WALK, hex, start, end, 0, step.isRoadStep())
              .getValue();
        assertEquals(bogs, target != TargetRoll.CHECK_FALSE);
        assertNotEquals(TargetRoll.AUTOMATIC_FAIL, target);
        assertEquals(bogs, Compute.isPilotingSkillNeeded(getGame(), tank.getId(), start, end,
              EntityMovementType.MOVE_WALK, false, false, 0, 0, step));
    }

    @ParameterizedTest
    @CsvSource({
          "pavement:1, 0, false, true", "road:1, 0, false, false", "road:1, 0, true, true",
          "road:3, 0, true, false", "road:4, 0, true, false", "pavement:1, 1, true, false",
          "bridge:1;bridge_elev:2, 0, true, false", "bridge:1;bridge_elev:2, 2, false, true"
    })
    void blackIceDependsOnWhichSurfaceTheUnitTouches(String surface, int elevation, boolean onRoad, boolean expected) {
        Hex hex = new Hex(0, surface, "");
        assertEquals(expected, RoadHazards.touchesPavement(hex, elevation, onRoad));
        try (var dice = mockStatic(Compute.class)) {
            dice.when(() -> Compute.d6(1)).thenReturn(5);
            assertEquals(expected, ServerHelper.checkEnteringBlackIce(mock(TWGameManager.class), new BipedMek(),
                  hex, elevation, onRoad, true));
            assertEquals(expected, hex.containsTerrain(Terrains.BLACK_ICE));
        }
    }

    @ParameterizedTest
    @CsvSource({
          "WHEELED, 0, FORWARDS, true", "WHEELED, 0, TURN_LEFT, false", "HOVER, 0, FORWARDS, false",
          "WIGE, 0, FORWARDS, false", "WIGE, 1, FORWARDS, false", "VTOL, 1, FORWARDS, false"
    })
    void recklessIceChecksRequireGroundContactOnEntry(EntityMovementMode mode, int elevation, MoveStepType type,
          boolean checksIce) {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 2
              hex 0101 0 "ice:1" ""
              hex 0102 0 "ice:1" ""
              end"""));
        Tank unit = mode.isVTOL() ? new VTOL() : new Tank();
        MovePath path = getMovePathFor(unit, elevation, mode);
        path.setCareful(false);
        MoveStep step = path.addStep(type).getLastStep();
        Hex hex = getGame().getBoard().getHex(step.getPosition());
        int roll = unit.checkRecklessMove(step, step.getMovementType(true), hex, start, step.getPosition(),
              getGame().getBoard().getHex(start), false).getValue();
        assertEquals(checksIce, roll != TargetRoll.CHECK_FALSE);
    }

    @ParameterizedTest
    @CsvSource({ "ice:1", "pavement:1;black_ice:1" })
    void aProtoMekUsesGunneryForRecklessEntryIntoExistingIce(String surface) {
        ProtoMek proto = new ProtoMek();
        drive(proto, EntityMovementMode.BIPED, "pavement:1", surface);
        proto.getCrew().setGunnery(2, 0);
        proto.getCrew().setPiloting(5, 0);
        MovePath path = new MovePath(getGame(), proto);
        path.setCareful(false);
        MoveStep step = path.addStep(MoveStepType.FORWARDS).getLastStep();
        assertEquals(2, proto.checkRecklessMove(step, step.getMovementType(true), getGame().getBoard().getHex(end),
              start, end, getGame().getBoard().getHex(start), false).getValue());
    }

    @Test
    void aHovercraftStillChecksForFogWhenEnteringAnIcyHex() {
        Tank hover = new Tank();
        drive(hover, EntityMovementMode.HOVER, "", "ice:1");
        getGame().getPlanetaryConditions().setFog(Fog.FOG_LIGHT);
        MovePath path = new MovePath(getGame(), hover);
        path.setCareful(false);
        MoveStep step = path.addStep(MoveStepType.FORWARDS).getLastStep();
        assertNotEquals(TargetRoll.CHECK_FALSE, hover.checkRecklessMove(step, step.getMovementType(true),
              getGame().getBoard().getHex(end), start, end, getGame().getBoard().getHex(start), false).getValue());
    }

    @Test
    void infantryAndHovercraftDoNotRevealBlackIce() {
        var manager = mock(TWGameManager.class);
        Hex pavement = new Hex(0, "pavement:1", "");
        Tank hover = new Tank();
        hover.setMovementMode(EntityMovementMode.HOVER);
        try (var dice = mockStatic(Compute.class)) {
            assertFalse(ServerHelper.checkEnteringBlackIce(manager, hover, pavement, 0, true, true));
            assertFalse(ServerHelper.checkEnteringBlackIce(manager, new ConvInfantry(), pavement, 0, true, true));
            dice.verify(() -> Compute.d6(1), never());
        }
    }

    private static final class EdgeFinder extends BoardEdgePathFinder {
        boolean allows(MovePath path) {
            return isLegalMove(path).isLegal();
        }
    }

    @ParameterizedTest
    @CsvSource({
          "true, road:1, 2, true", "true, road:1, -2, true", "false, road:1, 2, false",
          "true, road:3, 2, false", "true, road:4, -2, false", "true, '', 2, false"
    })
    void botRoutingUsesTheAssetsElevationLimitOnTheActualSurface(boolean core, String road, int level,
          boolean expected) {
        RulesManager previous = Game.rulesManager;
        try {
            getGame().setBoard(BoardLoader.initializeBoard("""
                  size 1 2
                  hex 0101 0 "%s" ""
                  hex 0102 %d "%s" ""
                  end""".formatted(road, level, road)));
            Game.rulesManager = core ? new CoreRulesManager() : new TWRulesManager();
            BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
            asset.setAssetType(BFSAssetType.VEHICLE);
            MovePath path = getMovePathFor(asset, 0, EntityMovementMode.TRACKED, MoveStepType.FORWARDS);
            assertEquals(expected, new EdgeFinder().allows(path));
            assertEquals(expected, WaypointDistanceField.stepCost(asset, getGame().getBoard(), start, end)
                  != WaypointDistanceField.UNREACHABLE);
            assertEquals(expected, WaypointDistanceField.build(asset, end).costFrom(start)
                  != WaypointDistanceField.UNREACHABLE);

            // The same tracked Asset has a different road limit when the rules change, even in a cached cluster.
            BoardClusterTracker tracker = new BoardClusterTracker();
            Game.rulesManager = new TWRulesManager();
            assertEquals(1, tracker.getBoardClusterSize(asset, start, false));
            Game.rulesManager = core ? new CoreRulesManager() : new TWRulesManager();
            assertEquals(expected ? 2 : 1, tracker.getBoardClusterSize(asset, start, false));
        } finally {
            Game.rulesManager = previous;
        }
    }

    @ParameterizedTest
    @CsvSource({
          "bridge:1:9;bridge_elev:1;bridge_cf:100, true, true",
          "bridge:1:8;bridge_elev:1;bridge_cf:100, false, true",
          "bridge:1:9;bridge_elev:1;bridge_cf:40, false, true",
          "bridge:1:9;bridge_elev:1;bridge_cf:100;water:1, true, true",
          "bridge:1:8;bridge_elev:3;bridge_cf:10;road:3;woods:2, true, false"
    })
    void waypointRoutingUsesTheReachableBridgeSurface(String bridge, boolean reachable, boolean useDeck) {
        Tank tank = new Tank();
        drive(tank, EntityMovementMode.WHEELED, "road:1;woods:2", bridge);
        var board = getGame().getBoard();
        MovePath path = new MovePath(getGame(), tank)
              .addStep(useDeck ? MoveStepType.CLIMB_MODE_ON : MoveStepType.CLIMB_MODE_OFF)
              .addStep(MoveStepType.FORWARDS);
        if (reachable) {
            assertTrue(path.isMoveLegal());
            assertEquals(useDeck ? 1 : 0, path.getFinalElevation());
        }
        assertEquals(reachable, WaypointDistanceField.stepCost(tank, board, start, end)
              != WaypointDistanceField.UNREACHABLE);
        assertEquals(reachable, WaypointDistanceField.build(tank, end).costFrom(start)
              != WaypointDistanceField.UNREACHABLE);
        assertEquals(reachable ? 2 : 1, new BoardClusterTracker().getBoardClusterSize(tank, start, false));
    }

    @ParameterizedTest
    @CsvSource({"1, true", "8, false"})
    void aTankOnClearGroundCanOnlyBoardAWaterSpanAtAnExit(int exits, boolean expected) {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "bridge:1:%d;bridge_elev:1;bridge_cf:100;water:1" ""
              end""".formatted(exits)));
        Tank tank = new Tank();
        MovePath path = getMovePathFor(tank, 0, EntityMovementMode.WHEELED, MoveStepType.CLIMB_MODE_ON,
              MoveStepType.FORWARDS);
        assertEquals(expected, path.isMoveLegal());
        assertEquals(expected, new EdgeFinder().allows(path));
        assertEquals(expected, WaypointDistanceField.build(tank, end).costFrom(tank, start)
              != WaypointDistanceField.UNREACHABLE);
        assertEquals(expected ? 2 : 1, new BoardClusterTracker().getBoardClusterSize(tank, start, false));
    }

    @Test
    void aUnitInTheUnderpassCannotDepartOnTheDecksOnlyRoad() {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 2
              hex 0101 0 "bridge:1:8;bridge_elev:2;bridge_cf:100" ""
              hex 0102 2 "road:1;woods:2" ""
              end"""));
        Tank tank = new Tank();
        getMovePathFor(tank, 0, EntityMovementMode.WHEELED);
        var board = getGame().getBoard();
        WaypointDistanceField field = WaypointDistanceField.build(tank, end);
        Princess princess = spy(new Princess("Bridge", UUID.randomUUID().toString(), 1));
        doReturn(getGame()).when(princess).getGame();

        assertEquals(WaypointDistanceField.UNREACHABLE, WaypointDistanceField.stepCost(tank, board, start, end));
        assertEquals(WaypointDistanceField.UNREACHABLE, field.costFrom(tank, start));
        assertFalse(princess.getUnitOrdersFollower().canReach(tank, end));

        tank.setElevation(2);
        assertNotEquals(WaypointDistanceField.UNREACHABLE, WaypointDistanceField.stepCost(tank, board, start, end));
        assertNotEquals(WaypointDistanceField.UNREACHABLE, field.costFrom(tank, start));
        assertTrue(princess.getUnitOrdersFollower().canReach(tank, end));
    }

    @Test
    void routeCannotSwitchBetweenUnderpassAndDeckInTheMiddleOfABridge() {
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 3
              hex 0101 2 "road:1;woods:2" ""
              hex 0102 0 "road:1;bridge:1:1;bridge_elev:2;bridge_cf:100" ""
              hex 0103 0 "road:1;woods:2" ""
              end"""));
        Tank tank = new Tank();
        getMovePathFor(tank, 0, EntityMovementMode.WHEELED);
        Coords south = new Coords(0, 2);
        var board = getGame().getBoard();
        assertNotEquals(WaypointDistanceField.UNREACHABLE, WaypointDistanceField.stepCost(tank, board, start, end));
        assertNotEquals(WaypointDistanceField.UNREACHABLE, WaypointDistanceField.stepCost(tank, board, end, south));
        assertEquals(WaypointDistanceField.UNREACHABLE, WaypointDistanceField.build(tank, south).costFrom(start));
    }

    @ParameterizedTest
    @CsvSource({
          "'', pavement:1;woods:2, false, false",
          "'', road:3;woods:2, false, false",
          "road:1, pavement:1;woods:2, true, true",
          "pavement:1;woods:2, '', false, true",
          "'', water:1, false, true",
          "road:1, road:1;impassable:1, false, false"
    })
    void displacementChecksTheApproachToTheDestination(String from, String into, boolean canDrive, boolean canPush) {
        Tank tank = new Tank();
        MovePath path = drive(tank, EntityMovementMode.WHEELED, from, into);
        assertEquals(canDrive, path.isMoveLegal());
        assertEquals(canPush, Compute.isValidDisplacement(getGame(), tank.getId(), start, end));
    }

    @Test
    void footAssetCannotBePushedOntoASnowyRoadWithoutItsSnowSpecial() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setAssetType(BFSAssetType.CONV_INFANTRY);
        drive(asset, EntityMovementMode.INF_LEG, "road:1", "road:1;snow:1");
        assertFalse(Compute.isValidDisplacement(getGame(), asset.getId(), start, end));
        asset.addSpecial(BFSSpecial.of("Snow"));
        assertTrue(Compute.isValidDisplacement(getGame(), asset.getId(), start, end));
        asset.setAssetType(BFSAssetType.EMPLACEMENT);
        assertFalse(Compute.isValidDisplacement(getGame(), asset.getId(), start, end));
    }

    @ParameterizedTest
    @CsvSource({"road:1, 2, true", "road:3, 2, false", "road:1, -3, true"})
    void displacementUsesTheAssetsPavedAscentLimitAndAllowsFalls(String road, int level, boolean expected) {
        RulesManager previous = Game.rulesManager;
        try {
            Game.rulesManager = new CoreRulesManager();
            getGame().setBoard(BoardLoader.initializeBoard("""
                  size 1 2
                  hex 0101 0 "%s" ""
                  hex 0102 %d "%s" ""
                  end""".formatted(road, level, road)));
            BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
            asset.setAssetType(BFSAssetType.VEHICLE);
            getMovePathFor(asset, 0, EntityMovementMode.TRACKED);
            assertEquals(expected, Compute.isValidDisplacement(getGame(), asset.getId(), start, end));
        } finally {
            Game.rulesManager = previous;
        }
    }

    @Test
    void waypointRoutingRequiresRoadsAtBothEndsOfAProhibitedWoodsCrossing() {
        Tank wheeled = new Tank();
        drive(wheeled, EntityMovementMode.WHEELED, "", "road:1;woods:2");
        assertEquals(WaypointDistanceField.UNREACHABLE,
              WaypointDistanceField.stepCost(wheeled, getGame().getBoard(), start, end));
        assertEquals(WaypointDistanceField.UNREACHABLE,
              WaypointDistanceField.stepCost(wheeled, getGame().getBoard(), end, start));
        assertEquals(1, new BoardClusterTracker().getBoardClusterSize(wheeled, start, false));
    }
}
