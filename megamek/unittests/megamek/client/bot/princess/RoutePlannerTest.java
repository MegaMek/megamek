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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.RouteStyle;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the planned way past a hill: the turning points a unit drives straight between, rather than heading at the
 * hill and along its foot (HammerGS's playtest, 2026-10-03).
 */
class RoutePlannerTest {

    private static final int WIDTH = 20;
    private static final int HEIGHT = 30;
    private static final int WALL_ROW = 15;
    // the wall runs from the west edge to here; east of it is the pass
    private static final int WALL_END_COLUMN = 13;
    private static final Coords START = new Coords(4, 26);
    private static final Coords TARGET = new Coords(4, 3);

    private Game game;
    private Board board;
    private Player bot;
    private Princess princess;
    private final List<Entity> owned = new ArrayList<>();

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        Hex[] hexes = new Hex[WIDTH * HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        board = new Board(WIDTH, HEIGHT, hexes);
        // a ridge too steep for a truck, with the pass at its east end
        for (int column = 0; column <= WALL_END_COLUMN; column++) {
            board.getHex(column, WALL_ROW).setLevel(3);
        }
        game = new Game();
        game.setBoard(board);
        game.setPhase(GamePhase.MOVEMENT);
        bot = new Player(1, "Princess-Bodkin");
        bot.setBot(true);
        game.addPlayer(1, bot);
        princess = spy(new Princess("Princess-Bodkin", UUID.randomUUID().toString(), 1));
        doReturn(game).when(princess).getGame();
        doReturn(owned).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
    }

    private Tank truck() {
        Tank truck = new Tank();
        truck.setId(3);
        truck.setOwner(bot);
        truck.setMovementMode(EntityMovementMode.WHEELED);
        truck.setOriginalWalkMP(4);
        truck.setWeight(35);
        game.addEntity(truck);
        truck.setPosition(START);
        truck.setDeployed(true);
        owned.add(truck);
        return truck;
    }

    @Test
    void theWayPastARidgeTurnsAtThePass() {
        Tank truck = truck();
        WaypointDistanceField field = WaypointDistanceField.build(truck, TARGET);

        List<Coords> turns = RoutePlanner.turningPoints(truck, field, START, board);

        assertFalse(turns.isEmpty(), "a ridge is in the way");
        Coords first = turns.get(0);
        assertTrue(first.getX() > WALL_END_COLUMN, "the first turn " + first + " is at the pass, past the ridge's end");
        assertTrue(Math.abs(first.getY() - WALL_ROW) <= 2, "the first turn " + first + " is at the pass");
        // the truck heads straight there rather than north to the ridge and along it
        assertTrue(RoutePlanner.isStraightAsCheap(truck, field, START, first, board));
    }

    @Test
    void anOpenWayIsAStraightLine() {
        for (int column = 0; column <= WALL_END_COLUMN; column++) {
            board.getHex(column, WALL_ROW).setLevel(0);
        }
        Tank truck = truck();
        WaypointDistanceField field = WaypointDistanceField.build(truck, TARGET);

        assertTrue(RoutePlanner.turningPoints(truck, field, START, board).isEmpty());
    }

    @Test
    void aLegSetToBePlannedGetsItsTurningPointsOnTheRoute() {
        Tank truck = truck();
        WaypointOrder planned = WaypointOrder.PASS_THROUGH.withRoutePlan(WaypointOrder.RoutePlan.PLAN_LEG);
        truck.setUnitOrders(UnitOrders.NONE.withRoute(List.of(TARGET), List.of(planned)));

        princess.getUnitOrdersFollower().advanceRoutes();

        UnitOrders orders = truck.getUnitOrders();
        List<Coords> route = orders.getRoute();
        assertTrue(route.size() > 1, "turning points went on the route: " + route);
        assertEquals(TARGET, route.get(route.size() - 1));
        assertTrue(orders.getWaypointOrder(0).isPlannedTurn());
        assertEquals(WaypointOrder.RoutePlan.PLAN_LEG, orders.getWaypointOrder(route.size() - 1).getRoutePlan());
    }

    @Test
    void aLegIsPlannedBeforeTheFirstMove() {
        // planned only after movement, a lance given its order before round 1 drove round 1 with no plan
        Tank truck = truck();
        WaypointOrder planned = WaypointOrder.PASS_THROUGH.withRoutePlan(WaypointOrder.RoutePlan.PLAN_LEG);
        truck.setUnitOrders(UnitOrders.NONE.withRoute(List.of(TARGET), List.of(planned)));

        princess.getUnitOrdersFollower().planRoutes();

        assertTrue(truck.getUnitOrders().getRoute().size() > 1, "turning points before the first move");
        assertTrue(truck.getUnitOrders().getWaypointOrder(0).isPlannedTurn());
    }

    @Test
    void aPausedLanceIsNotPlannedOutOfItsPause() {
        // a planned leg is sent as a new route, and a new route clears a pause: planning a paused truck let it drive on
        Tank truck = truck();
        WaypointOrder planned = WaypointOrder.PASS_THROUGH.withRoutePlan(WaypointOrder.RoutePlan.PLAN_LEG);
        truck.setUnitOrders(UnitOrders.NONE.withRoute(List.of(TARGET), List.of(planned)).withPaused(true));

        princess.getUnitOrdersFollower().advanceRoutes();

        assertTrue(truck.getUnitOrders().isPaused());
        assertEquals(List.of(TARGET), truck.getUnitOrders().getRoute());

        truck.setUnitOrders(truck.getUnitOrders().withPaused(false));
        princess.getUnitOrdersFollower().advanceRoutes();

        assertTrue(truck.getUnitOrders().getRoute().size() > 1, "planned once it moves on");
    }

    @Test
    void aLegNotSetToBePlannedIsLeftAlone() {
        Tank truck = truck();
        truck.setUnitOrders(UnitOrders.NONE.withRoute(List.of(TARGET)));

        princess.getUnitOrdersFollower().advanceRoutes();

        assertEquals(List.of(TARGET), truck.getUnitOrders().getRoute());
    }

    @Test
    void thePlanTravelsInRouteText() {
        WaypointOrder planned = WaypointOrder.PASS_THROUGH.withRoutePlan(WaypointOrder.RoutePlan.PLAN_LEG);
        WaypointOrder turn = WaypointOrder.PASS_THROUGH.withRoutePlan(WaypointOrder.RoutePlan.TURN_POINT);

        assertEquals("/PLAN", planned.toCommandSuffix());
        assertEquals(planned, WaypointOrder.parse(List.of("PLAN")));
        assertEquals(turn, WaypointOrder.parse(List.of("TURN")));
        assertEquals(WaypointOrder.RoutePlan.NONE, WaypointOrder.parse(List.of("NE")).getRoutePlan());
    }

    @Test
    void aCoveredWayKeepsToWoodsBesideTheStraightLine() {
        for (int column = 0; column <= WALL_END_COLUMN; column++) {
            board.getHex(column, WALL_ROW).setLevel(0);
        }
        // a strip of light woods two columns east of the straight line
        int woodsColumn = START.getX() + 2;
        for (int row = 4; row <= 25; row++) {
            board.getHex(woodsColumn, row).addTerrain(new Terrain(Terrains.WOODS, 1));
        }
        BipedMek mek = new BipedMek();
        mek.setId(4);
        mek.setOwner(bot);
        game.addEntity(mek);
        mek.setPosition(START);

        List<Coords> fastest = RoutePlanner.plan(mek, START, TARGET, RouteStyle.FASTEST);
        List<Coords> covered = RoutePlanner.plan(mek, START, TARGET, RouteStyle.COVERED);

        assertEquals(List.of(TARGET), fastest, "open ground: the fastest way is straight");
        assertTrue(covered.size() > 1, "the covered way turns for the woods: " + covered);
        assertEquals(TARGET, covered.get(covered.size() - 1));
        assertTrue(RoutePlanner.isDefensiveGround(board, new Coords(woodsColumn, 10)));
        assertFalse(RoutePlanner.isDefensiveGround(board, new Coords(START.getX(), 10)));
    }

    @Test
    void theStyleTravelsInRouteText() {
        WaypointOrder covered = WaypointOrder.PASS_THROUGH.withRoutePlan(WaypointOrder.RoutePlan.PLAN_LEG)
              .withRouteStyle(RouteStyle.COVERED);

        assertEquals("/PLAN:COVERED", covered.toCommandSuffix());
        assertEquals(covered, WaypointOrder.parse(List.of("PLAN:COVERED")));
        assertEquals(RouteStyle.FASTEST, WaypointOrder.parse(List.of("PLAN")).getRouteStyle());
    }

    @Test
    void aColumnStillFormingUpWaitsAtItsFirstPlannedTurn() {
        for (int column = 0; column <= WALL_END_COLUMN; column++) {
            board.getHex(column, WALL_ROW).setLevel(0);
        }
        Tank leader = truck();
        Coords turn = new Coords(START.getX(), START.getY() - 6);
        leader.setPosition(turn);
        WaypointOrder turnOrder = WaypointOrder.PASS_THROUGH.withRoutePlan(WaypointOrder.RoutePlan.TURN_POINT);
        FormationOrder column = new FormationOrder(FormationShape.COLUMN, leader.getId(), 1, 0, FormationPace.WALK,
              ContactRule.HOLD);
        leader.setUnitOrders(UnitOrders.NONE.withRoute(List.of(turn, TARGET), List.of(turnOrder,
              WaypointOrder.PASS_THROUGH)).withFormation(column));
        Tank follower = new Tank();
        follower.setId(5);
        follower.setOwner(bot);
        follower.setMovementMode(EntityMovementMode.WHEELED);
        follower.setOriginalWalkMP(4);
        follower.setWeight(35);
        game.addEntity(follower);
        // far behind: the column has not formed up
        follower.setPosition(new Coords(START.getX() + 6, START.getY()));
        follower.setDeployed(true);
        follower.setUnitOrders(UnitOrders.NONE.withFormation(new FormationOrder(FormationShape.COLUMN, leader.getId(),
              1, 1, FormationPace.WALK, ContactRule.HOLD)));
        owned.add(follower);
        UnitOrdersFollower orders = princess.getUnitOrdersFollower();

        orders.advanceRoutes();
        assertEquals(turn, leader.getUnitOrders().getRoute().get(0), "it waits at the first turn to form up");

        orders.noteAssembled(leader.getId());
        orders.advanceRoutes();
        assertEquals(TARGET, leader.getUnitOrders().getRoute().get(0), "formed up, it passes straight through");
    }
}
