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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.common.OffBoardDirection;
import megamek.common.board.Coords;
import megamek.common.enums.ForcedWithdrawalOrder;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #9038: a crippled bot unit ignored the player's waypoint and withdrew to the nearest
 * edge, a flee order sent crippled units to their retreat edge instead of the ordered edge, and a unit following a
 * waypoint was scored by its distance to the home edge instead of the waypoint.
 */
class UnitBehaviorOrdersTest {

    private static final Coords NORTH_WAYPOINT = new Coords(14, 1);
    private static final Coords UNREACHABLE_HEX = new Coords(30, 30);

    private Princess princess;

    @BeforeEach
    void setUp() {
        princess = spy(new Princess("TestPrincess", UUID.randomUUID().toString(), 1));
        BoardClusterTracker clusterTracker = mock(BoardClusterTracker.class);
        doReturn(clusterTracker).when(princess).getClusterTracker();
        doReturn(true).when(princess).getForcedWithdrawal();
        // the bot sends its order changes to the server as chat commands; there is no server here
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
        // every edge and hex is reachable unless a test says otherwise
        when(clusterTracker.getDestinationCoords(any(Entity.class), any(CardinalEdge.class), anyBoolean()))
              .thenReturn(Set.of(new Coords(0, 0)));
        when(clusterTracker.getDestinationCoords(any(Entity.class), any(Coords.class), anyBoolean()))
              .thenReturn(Set.of(new Coords(0, 0)));
        when(clusterTracker.getDestinationCoords(any(Entity.class), eq(UNREACHABLE_HEX), anyBoolean()))
              .thenReturn(Collections.emptySet());
        princess.getBehaviorSettings().setRetreatEdge(CardinalEdge.SOUTH);
    }

    private static Entity unit(int unitId, boolean isCrippled, ForcedWithdrawalOrder order) {
        Game game = mock(Game.class);
        when(game.getCurrentRound()).thenReturn(4);
        when(game.getAllEnemyEntities(any(Entity.class))).thenReturn(Collections.emptyIterator());
        Entity unit = mock(Entity.class);
        when(unit.getId()).thenReturn(unitId);
        when(unit.getDisplayName()).thenReturn("Champion CHP-2N #" + unitId);
        when(unit.isCrippled(true)).thenReturn(isCrippled);
        when(unit.getForcedWithdrawalOrder()).thenReturn(order);
        when(unit.getGame()).thenReturn(game);
        when(unit.getBoardId()).thenReturn(0);
        when(unit.getMovementMode()).thenReturn(EntityMovementMode.BIPED);
        // orders live on the unit, so the mock keeps them the way a real unit would
        AtomicReference<UnitOrders> orders = new AtomicReference<>(UnitOrders.NONE);
        when(unit.getUnitOrders()).thenAnswer(invocation -> orders.get());
        doAnswer(invocation -> {
            orders.set(invocation.getArgument(0));
            return null;
        }).when(unit).setUnitOrders(any());
        return unit;
    }

    private void orderFleeNorth() {
        princess.getBehaviorSettings().setAutoFlee(true);
        princess.getBehaviorSettings().setDestinationEdge(CardinalEdge.NORTH);
    }

    @Test
    void aCrippledUnitFollowsThePlayersWaypoint() {
        Entity crippled = unit(146, true, ForcedWithdrawalOrder.BOT_RULES);
        princess.getUnitBehaviorTracker().setEntityWaypoints(crippled, List.of(NORTH_WAYPOINT), princess);

        assertEquals(BehaviorType.MoveToDestination,
              princess.getUnitBehaviorTracker().getBehaviorType(crippled, princess));
        assertEquals(Optional.of(NORTH_WAYPOINT),
              princess.getUnitBehaviorTracker().getActiveWaypoint(crippled, princess));
    }

    @Test
    void aCrippledUnitWithoutWaypointsStillWithdraws() {
        Entity crippled = unit(146, true, ForcedWithdrawalOrder.BOT_RULES);

        assertEquals(BehaviorType.ForcedWithdrawal,
              princess.getUnitBehaviorTracker().getBehaviorType(crippled, princess));
        assertEquals(CardinalEdge.SOUTH, princess.getHomeEdge(crippled));
    }

    @Test
    void aGamemastersWithdrawOrderBeatsTheWaypoint() {
        Entity ordered = unit(146, false, ForcedWithdrawalOrder.WITHDRAW);
        princess.getUnitBehaviorTracker().setEntityWaypoints(ordered, List.of(NORTH_WAYPOINT), princess);

        assertEquals(BehaviorType.ForcedWithdrawal,
              princess.getUnitBehaviorTracker().getBehaviorType(ordered, princess));
        assertFalse(princess.getUnitBehaviorTracker().isFollowingOrdersOverWithdrawal(ordered, princess));
    }

    @Test
    void aFleeOrderSendsACrippledUnitToTheOrderedEdge() {
        // #9038: "flee north" sent the crippled Champion south, to its retreat edge
        orderFleeNorth();
        Entity crippled = unit(146, true, ForcedWithdrawalOrder.BOT_RULES);

        assertEquals(CardinalEdge.NORTH, princess.getHomeEdge(crippled));
        assertEquals(BehaviorType.ForcedWithdrawal,
              princess.getUnitBehaviorTracker().getBehaviorType(crippled, princess));
    }

    @Test
    void aFleeOrderStillBeatsWaypoints() {
        orderFleeNorth();
        Entity healthy = unit(147, false, ForcedWithdrawalOrder.BOT_RULES);
        princess.getUnitBehaviorTracker().setEntityWaypoints(healthy, List.of(new Coords(5, 30)), princess);

        assertEquals(BehaviorType.MoveToDestination,
              princess.getUnitBehaviorTracker().getBehaviorType(healthy, princess));
        assertTrue(princess.getUnitBehaviorTracker().getActiveWaypoint(healthy, princess).isEmpty());
        assertEquals(CardinalEdge.NORTH, princess.getHomeEdge(healthy));
    }

    @Test
    void settingWaypointsReportsHowManyWereKept() {
        Entity healthy = unit(147, false, ForcedWithdrawalOrder.BOT_RULES);

        int keptCount = princess.getUnitBehaviorTracker()
              .setEntityWaypoints(healthy, List.of(NORTH_WAYPOINT, UNREACHABLE_HEX), princess);

        assertEquals(1, keptCount);
        assertEquals(0, princess.getUnitBehaviorTracker()
              .addEntityWaypoint(healthy, List.of(UNREACHABLE_HEX), princess));
    }

    @Test
    void aWaypointUnitIsScoredByItsDistanceToTheWaypoint() {
        // With no destination edge the home edge fell back to NORTH, so a waypoint to the south was scored as if
        // the unit should head north.
        Entity healthy = unit(147, false, ForcedWithdrawalOrder.BOT_RULES);
        Coords southWaypoint = new Coords(10, 30);
        princess.getUnitBehaviorTracker().setEntityWaypoints(healthy, List.of(southWaypoint), princess);
        PathRanker ranker = new PathRanker(princess) {
            @Override
            protected RankedPath rankPath(MovePath path, Game game, int maxRange, double fallTolerance,
                  List<Entity> enemies, Coords friendsCoords) {
                return null;
            }

            @Override
            public double distanceToClosestEnemy(Entity entity, Coords position, Game game) {
                return 0;
            }
        };
        Game game = mock(Game.class);

        Coords nearWaypoint = new Coords(10, 25);
        Coords farFromWaypoint = new Coords(10, 5);
        int nearDistance = ranker.distanceToDestination(healthy, nearWaypoint, 0, game);
        int farDistance = ranker.distanceToDestination(healthy, farFromWaypoint, 0, game);

        assertEquals(nearWaypoint.distance(southWaypoint) - Princess.DISTANCE_TO_WAYPOINT, nearDistance);
        assertTrue(nearDistance < farDistance);
        assertEquals(0, ranker.distanceToDestination(healthy, new Coords(10, 29), 0, game));
    }

    private static void giveEdgeOrder(Entity unit, UnitOrderAction action, OffBoardDirection edge) {
        unit.setUnitOrders(action.apply(unit.getUnitOrders(), List.of(), edge, UnitOrders.FACING_AUTO,
              UnitOrders.FACING_AUTO, null, 4));
    }

    @Test
    void anEdgeOrderSendsEvenACrippledUnitToThatEdge() {
        Entity crippled = unit(146, true, ForcedWithdrawalOrder.BOT_RULES);
        giveEdgeOrder(crippled, UnitOrderAction.MOVE_TO_EDGE, OffBoardDirection.NORTH);

        assertEquals(BehaviorType.MoveToDestination,
              princess.getUnitBehaviorTracker().getBehaviorType(crippled, princess));
        assertEquals(CardinalEdge.NORTH, princess.getHomeEdge(crippled));
        assertTrue(princess.getUnitBehaviorTracker().isFollowingOrdersOverWithdrawal(crippled, princess));
    }

    @Test
    void aGamemastersWithdrawOrderBeatsAnEdgeOrder() {
        Entity ordered = unit(146, false, ForcedWithdrawalOrder.WITHDRAW);
        giveEdgeOrder(ordered, UnitOrderAction.EXIT_BY_EDGE, OffBoardDirection.NORTH);

        assertEquals(BehaviorType.ForcedWithdrawal,
              princess.getUnitBehaviorTracker().getBehaviorType(ordered, princess));
        assertEquals(CardinalEdge.SOUTH, princess.getHomeEdge(ordered));
    }

    @Test
    void aUnitOrderedToAnEdgeToHoldThereNeverLeavesTheBoard() {
        Entity healthy = unit(147, false, ForcedWithdrawalOrder.BOT_RULES);
        giveEdgeOrder(healthy, UnitOrderAction.MOVE_TO_EDGE, OffBoardDirection.NORTH);

        assertFalse(princess.mustFleeBoard(healthy));
    }

    @Test
    void anEdgeOrderBeatsTheBotWideFleeOrder() {
        orderFleeNorth();
        Entity healthy = unit(147, false, ForcedWithdrawalOrder.BOT_RULES);
        giveEdgeOrder(healthy, UnitOrderAction.MOVE_TO_EDGE, OffBoardDirection.EAST);

        assertEquals(CardinalEdge.EAST, princess.getHomeEdge(healthy));
    }

    @Test
    void pauseAndStopHoldTheUnit() {
        princess.getGame().setCurrentRound(6);
        Entity paused = unit(147, false, ForcedWithdrawalOrder.BOT_RULES);
        paused.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)).withPaused(true));
        Entity stoppedLastRound = unit(148, false, ForcedWithdrawalOrder.BOT_RULES);
        stoppedLastRound.setUnitOrders(UnitOrders.stoppedInRound(princess.getGame().getCurrentRound() - 1));
        Entity stoppedThisRound = unit(149, false, ForcedWithdrawalOrder.BOT_RULES);
        stoppedThisRound.setUnitOrders(UnitOrders.stoppedInRound(princess.getGame().getCurrentRound()));

        assertTrue(princess.getUnitOrdersFollower().isHolding(paused));
        assertFalse(princess.getUnitOrdersFollower().isHolding(stoppedLastRound));
        assertTrue(princess.getUnitOrdersFollower().isHolding(stoppedThisRound));
    }

    @Test
    void anImperativeRoutePullsHarder() {
        Entity normal = unit(147, false, ForcedWithdrawalOrder.BOT_RULES);
        normal.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)));
        Entity imperative = unit(148, false, ForcedWithdrawalOrder.BOT_RULES);
        imperative.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT))
              .withPriority(OrderPriority.IMPERATIVE));

        assertEquals(1.0, princess.getUnitOrdersFollower().routeWeight(normal));
        assertEquals(UnitOrdersFollower.IMPERATIVE_ROUTE_WEIGHT,
              princess.getUnitOrdersFollower().routeWeight(imperative));
    }

    @Test
    void anOrderedFacingStandsWhileTheThreatIsInItsFrontArc() {
        // The three cases from the design: Atlas ordered to face NE (1) at 1508.
        Coords position = new Coords(14, 7);
        int northEast = 1;
        Coords threatToTheNorth = new Coords(14, 2);
        Coords threatToTheSouthWest = new Coords(10, 10);

        assertEquals(northEast, UnitOrdersFollower.facingThatStands(northEast, position, null));
        assertEquals(northEast, UnitOrdersFollower.facingThatStands(northEast, position, threatToTheNorth));
        assertEquals(UnitOrders.FACING_AUTO,
              UnitOrdersFollower.facingThatStands(northEast, position, threatToTheSouthWest));
    }

    @Test
    void theStoppedFacingAppliesOnlyAtTheLastWaypoint() {
        Entity healthy = unit(147, false, ForcedWithdrawalOrder.BOT_RULES);
        healthy.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)).withFacings(0, 1));

        assertEquals(0, princess.getUnitOrdersFollower().orderedFacing(healthy, new Coords(14, 20)));
        assertEquals(1, princess.getUnitOrdersFollower().orderedFacing(healthy, new Coords(14, 2)));
    }
}
