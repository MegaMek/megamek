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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.common.Hex;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.orders.LanceRole;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
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
 * Tests what a convoy does on its own: leaves by its exit edge once its route is done, waits when paused, and takes
 * roads at road cost (HammerGS, 2026-10-03).
 */
class ConvoyBehaviourTest {

    private static final int WIDTH = 20;
    private static final int HEIGHT = 30;
    private static final int ROAD_ROW = 10;
    private static final Coords START = new Coords(9, 25);
    private static final Coords WAYPOINT = new Coords(9, 15);

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

    private Tank truck(int unitId, EntityMovementMode mode, boolean isConvoy) {
        Tank truck = new Tank();
        truck.setId(unitId);
        truck.setOwner(bot);
        truck.setMovementMode(mode);
        truck.setOriginalWalkMP(4);
        truck.setWeight(35);
        game.addEntity(truck);
        truck.setPosition(START);
        truck.setDeployed(true);
        if (isConvoy) {
            truck.setLanceRole(LanceRole.convoy(OffBoardDirection.NORTH));
        }
        owned.add(truck);
        return truck;
    }

    @Test
    void aConvoyWithNoRouteHeadsOffItsExitEdge() {
        Tank truck = truck(3, EntityMovementMode.WHEELED, true);

        princess.getUnitOrdersFollower().advanceRoutes();

        UnitOrders orders = truck.getUnitOrders();
        assertEquals(List.of(new Coords(START.getX(), 0)), orders.getRoute());
        assertTrue(orders.getWaypointOrder(0).isExitBoard(), "the edge waypoint is set to exit");
    }

    @Test
    void aRouteEndingOnTheExitEdgeLeavesThereWithoutASecondWaypoint() {
        // HammerGS's playtest, 2026-10-04: a route to 1701 on the north edge became 1701, then 1701 again
        Tank truck = truck(3, EntityMovementMode.WHEELED, true);
        Coords onTheEdge = new Coords(WAYPOINT.getX(), 0);
        truck.setUnitOrders(UnitOrders.NONE.withRoute(List.of(WAYPOINT, onTheEdge)));

        princess.getUnitOrdersFollower().advanceRoutes();

        assertEquals(List.of(WAYPOINT, onTheEdge), truck.getUnitOrders().getRoute());
        assertTrue(truck.getUnitOrders().getWaypointOrder(1).isExitBoard());
    }

    @Test
    void aConvoyFollowsItsRouteThenLeaves() {
        Tank truck = truck(3, EntityMovementMode.WHEELED, true);
        truck.setUnitOrders(UnitOrders.NONE.withRoute(List.of(WAYPOINT)));

        princess.getUnitOrdersFollower().advanceRoutes();

        assertEquals(List.of(WAYPOINT, new Coords(WAYPOINT.getX(), 0)), truck.getUnitOrders().getRoute());
        assertTrue(truck.getUnitOrders().getWaypointOrder(1).isExitBoard());
    }

    @Test
    void aPausedConvoyWaits() {
        Tank truck = truck(3, EntityMovementMode.WHEELED, true);
        truck.setUnitOrders(UnitOrderAction.PAUSE.apply(UnitOrders.NONE, List.of(), OffBoardDirection.NONE,
              UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, 0));

        princess.getUnitOrdersFollower().advanceRoutes();

        assertTrue(truck.getUnitOrders().getRoute().isEmpty());
    }

    @Test
    void aLanceThatIsNotAConvoyKeepsItsRoute() {
        Tank truck = truck(3, EntityMovementMode.WHEELED, false);
        truck.setUnitOrders(UnitOrders.NONE.withRoute(List.of(WAYPOINT)));

        princess.getUnitOrdersFollower().advanceRoutes();

        assertEquals(List.of(WAYPOINT), truck.getUnitOrders().getRoute());
    }

    @Test
    void aConvoyTakesARoadThroughWoodsAtRoadCost() {
        for (int column = 0; column < WIDTH; column++) {
            for (int row = 0; row < HEIGHT; row++) {
                board.getHex(column, row).addTerrain(new Terrain(Terrains.WOODS, 1));
            }
            board.getHex(column, ROAD_ROW).addTerrain(new Terrain(Terrains.ROAD, 1));
        }
        Tank convoy = truck(3, EntityMovementMode.TRACKED, true);
        Tank other = truck(4, EntityMovementMode.TRACKED, false);
        Coords roadStart = new Coords(2, ROAD_ROW);
        Coords roadEnd = new Coords(17, ROAD_ROW);

        int convoyCost = WaypointDistanceField.build(convoy, roadEnd).costFrom(roadStart);
        int otherCost = WaypointDistanceField.build(other, roadEnd).costFrom(roadStart);

        assertEquals(roadStart.distance(roadEnd), convoyCost, "a step a road hex: 1 MP");
        assertTrue(otherCost > convoyCost, "without the road discount the woods cost more: " + otherCost);
    }

    @Test
    void aMekInAConvoyLanceIsRoutedOutToo() {
        BipedMek mek = new BipedMek();
        mek.setId(5);
        mek.setOwner(bot);
        game.addEntity(mek);
        mek.setPosition(START);
        mek.setDeployed(true);
        mek.setLanceRole(LanceRole.convoy(OffBoardDirection.NORTH));
        owned.add(mek);

        princess.getUnitOrdersFollower().advanceRoutes();

        assertEquals(List.of(new Coords(START.getX(), 0)), mek.getUnitOrders().getRoute());
    }

    @Test
    void aConvoySetToWaitStaysAtTheEndOfItsRoute() {
        Tank truck = truck(3, EntityMovementMode.WHEELED, true);
        truck.setLanceRole(LanceRole.convoy(OffBoardDirection.NORTH, true));
        truck.setUnitOrders(UnitOrders.NONE.withRoute(List.of(WAYPOINT)));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        follower.advanceRoutes();
        assertEquals(List.of(WAYPOINT), truck.getUnitOrders().getRoute(), "no exit goes on its route");

        truck.setUnitOrders(UnitOrders.NONE);
        follower.advanceRoutes();
        assertTrue(truck.getUnitOrders().getRoute().isEmpty());
        assertTrue(follower.isHolding(truck), "with no route left it holds where it is");
    }
}
