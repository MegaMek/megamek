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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.common.board.Coords;
import megamek.common.enums.ForcedWithdrawalOrder;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.units.Entity;
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
        assertFalse(princess.getUnitBehaviorTracker().isFollowingWaypointOverWithdrawal(ordered, princess));
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
}
