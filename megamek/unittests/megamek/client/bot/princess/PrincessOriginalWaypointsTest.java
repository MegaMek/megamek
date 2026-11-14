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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import megamek.client.bot.caspar.Caspar;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.common.OffBoardDirection;
import megamek.common.board.Coords;
import megamek.common.enums.ForcedWithdrawalOrder;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Princess keeps the waypoints she had before unit orders (#7615): held in her own memory, scored by straight-line
 * distance, and blind to any unit orders a unit carries. CASPAR follows unit orders. These tests pin Princess's side of
 * that split, so she stays the unchanged control CASPAR is measured against.
 */
class PrincessOriginalWaypointsTest {

    private static final Coords NORTH_WAYPOINT = new Coords(14, 1);

    private Princess princess;

    @BeforeEach
    void setUp() {
        princess = spy(new Princess("TestPrincess", UUID.randomUUID().toString(), 1));
        BoardClusterTracker clusterTracker = mock(BoardClusterTracker.class);
        doReturn(clusterTracker).when(princess).getClusterTracker();
        doReturn(true).when(princess).getForcedWithdrawal();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
        // every edge and hex is reachable
        when(clusterTracker.getDestinationCoords(any(Entity.class), any(CardinalEdge.class), anyBoolean()))
              .thenReturn(Set.of(new Coords(0, 0)));
        when(clusterTracker.getDestinationCoords(any(Entity.class), any(Coords.class), anyBoolean()))
              .thenReturn(Set.of(new Coords(0, 0)));
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
        when(unit.getPosition()).thenReturn(new Coords(10, 20));
        when(unit.getMovementMode()).thenReturn(EntityMovementMode.BIPED);
        AtomicReference<UnitOrders> orders = new AtomicReference<>(UnitOrders.NONE);
        when(unit.getUnitOrders()).thenAnswer(invocation -> orders.get());
        doAnswer(invocation -> {
            orders.set(invocation.getArgument(0));
            return null;
        }).when(unit).setUnitOrders(any());
        return unit;
    }

    private static PathRanker rankerFor(Princess owner) {
        return new PathRanker(owner) {
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
    }

    @Test
    void princessKeepsWaypointsInHerOwnMemoryAndCasparOnTheUnits() {
        Princess caspar = new Caspar("CASPAR", "localhost", 0);

        assertFalse(princess.followsUnitOrders());
        assertFalse(princess.getUnitBehaviorTracker() instanceof UnitOrdersBehavior);
        assertTrue(caspar.followsUnitOrders());
        assertInstanceOf(UnitOrdersBehavior.class, caspar.getUnitBehaviorTracker());
    }

    @Test
    void princessWaypointsNeverTouchTheUnitsOrders() {
        Entity healthy = unit(147, false, ForcedWithdrawalOrder.BOT_RULES);

        int keptCount = princess.getUnitBehaviorTracker()
              .setEntityWaypoints(healthy, List.of(NORTH_WAYPOINT), princess);

        assertEquals(1, keptCount);
        assertEquals(Optional.of(NORTH_WAYPOINT), princess.getUnitBehaviorTracker().getWaypointForEntity(healthy));
        assertTrue(healthy.getUnitOrders().isEmpty());
        // nothing is sent to the server: Princess's waypoints are not unit orders
        verify(princess, never()).sendChat(anyString());
    }

    @Test
    void aCrippledUnitFollowsItsWaypointAndAGamemastersWithdrawStillWins() {
        Entity crippled = unit(146, true, ForcedWithdrawalOrder.BOT_RULES);
        Entity ordered = unit(148, false, ForcedWithdrawalOrder.WITHDRAW);
        princess.getUnitBehaviorTracker().setEntityWaypoints(crippled, List.of(NORTH_WAYPOINT), princess);
        princess.getUnitBehaviorTracker().setEntityWaypoints(ordered, List.of(NORTH_WAYPOINT), princess);

        assertEquals(BehaviorType.MoveToDestination,
              princess.getUnitBehaviorTracker().getBehaviorType(crippled, princess));
        assertEquals(BehaviorType.ForcedWithdrawal,
              princess.getUnitBehaviorTracker().getBehaviorType(ordered, princess));
    }

    @Test
    void princessIgnoresUnitOrdersAUnitCarries() {
        // a unit handed over from a CASPAR bot keeps its orders on the unit; Princess goes by her own rules
        Entity crippled = unit(146, true, ForcedWithdrawalOrder.BOT_RULES);
        crippled.setUnitOrders(UnitOrderAction.EXIT_BY_EDGE.apply(UnitOrders.NONE, List.of(),
              OffBoardDirection.NORTH, UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, 4));
        crippled.setUnitOrders(UnitOrderAction.ROUTE.apply(crippled.getUnitOrders(), List.of(NORTH_WAYPOINT),
              OffBoardDirection.NONE, UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, 4));

        assertEquals(CardinalEdge.SOUTH, princess.getHomeEdge(crippled));
        assertEquals(BehaviorType.ForcedWithdrawal,
              princess.getUnitBehaviorTracker().getBehaviorType(crippled, princess));
        assertFalse(princess.mustFleeBoard(crippled));
    }

    @Test
    void aWaypointIsScoredInAStraightLine() {
        Entity healthy = unit(147, false, ForcedWithdrawalOrder.BOT_RULES);
        Coords southWaypoint = new Coords(10, 30);
        princess.getUnitBehaviorTracker().setEntityWaypoints(healthy, List.of(southWaypoint), princess);
        PathRanker ranker = rankerFor(princess);
        Game game = mock(Game.class);
        Coords position = new Coords(10, 5);

        assertEquals(position.distance(southWaypoint) - Princess.DISTANCE_TO_WAYPOINT,
              ranker.distanceToDestination(healthy, position, 0, game));
        assertEquals(0, ranker.distanceToDestination(healthy, new Coords(10, 29), 0, game));
    }

    @Test
    void aUnitWithinThreeHexesOfItsWaypointMovesOnToTheNext() {
        Entity healthy = unit(147, false, ForcedWithdrawalOrder.BOT_RULES);
        Coords nearWaypoint = new Coords(10, 22);
        princess.getUnitBehaviorTracker()
              .setEntityWaypoints(healthy, List.of(nearWaypoint, NORTH_WAYPOINT), princess);
        doReturn(List.of(healthy)).when(princess).getEntitiesOwned();

        princess.postMovementProcessing();

        assertEquals(Optional.of(NORTH_WAYPOINT), princess.getUnitBehaviorTracker().getWaypointForEntity(healthy));
    }
}
