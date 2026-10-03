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
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
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
}
