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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import megamek.common.Hex;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.LanceRole;
import megamek.common.orders.UnitOrders;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests where a lance deploys when units are blocked: never across water a wheeled unit cannot cross from where its
 * lance is going, and round the hex its formation's first unit down picked, whatever order the turns come in
 * (HammerGS's playtest, 2026-10-03).
 */
class DeploymentReachTest {

    private static final int WIDTH = 20;
    private static final int HEIGHT = 20;
    private static final int RIVER_COLUMN = 8;
    private static final int LEADER_ID = 3;
    private static final Coords WEST_BANK = new Coords(3, 18);
    private static final Coords EAST_BANK = new Coords(13, 18);
    private static final Coords FIRST_WAYPOINT = new Coords(14, 4);

    private Game game;
    private Player bot;
    private Princess princess;

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
        Board board = new Board(WIDTH, HEIGHT, hexes);
        // a river the length of the board, as on the map the convoy's MASH truck deployed across
        for (int row = 0; row < HEIGHT; row++) {
            board.getHex(RIVER_COLUMN, row).addTerrain(new Terrain(Terrains.WATER, 1));
        }
        game = new Game();
        game.setBoard(board);
        bot = new Player(1, "Lyran Allies");
        bot.setBot(true);
        game.addPlayer(1, bot);
        princess = spy(new Princess("Lyran Allies", UUID.randomUUID().toString(), 1));
        doReturn(game).when(princess).getGame();
    }

    private Tank truck(int unitId, int slot) {
        Tank truck = new Tank();
        truck.setId(unitId);
        truck.setOwner(bot);
        truck.setMovementMode(EntityMovementMode.WHEELED);
        truck.setOriginalWalkMP(4);
        truck.setWeight(35);
        game.addEntity(truck);
        truck.setUnitOrders(UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT)).withFormation(
              new FormationOrder(FormationShape.COLUMN, LEADER_ID, 1, slot, FormationPace.RUN, ContactRule.HOLD)));
        return truck;
    }

    @Test
    void aWheeledTruckNeverDeploysAcrossARiverFromItsRoute() {
        Tank truck = truck(LEADER_ID, 0);

        List<Coords> kept = princess.getUnitOrdersFollower().keepReachable(truck, List.of(WEST_BANK, EAST_BANK));

        assertEquals(List.of(EAST_BANK), kept);
    }

    @Test
    void aMekThatCanWadeKeepsBothBanks() {
        BipedMek mek = new BipedMek();
        mek.setId(10);
        mek.setOwner(bot);
        game.addEntity(mek);
        mek.setUnitOrders(UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT)));

        List<Coords> kept = princess.getUnitOrdersFollower().keepReachable(mek, List.of(WEST_BANK, EAST_BANK));

        assertEquals(List.of(WEST_BANK, EAST_BANK), kept);
    }

    @Test
    void aLeaderKeepsOnlyHexesEveryMemberCanDriveFrom() {
        BipedMek leader = new BipedMek();
        leader.setId(LEADER_ID);
        leader.setOwner(bot);
        game.addEntity(leader);
        leader.setUnitOrders(UnitOrders.NONE.withRoute(List.of(FIRST_WAYPOINT)).withFormation(
              new FormationOrder(FormationShape.COLUMN, LEADER_ID, 1, 0, FormationPace.RUN, ContactRule.HOLD)));
        truck(4, 1);

        // the leader could wade the river, but its truck could not follow
        List<Coords> kept = princess.getUnitOrdersFollower().keepReachable(leader, List.of(WEST_BANK, EAST_BANK));

        assertEquals(List.of(EAST_BANK), kept);
    }

    @Test
    void anEscortIsKeptOnItsConvoysSideOfTheRiver() {
        Tank convoyLead = truck(LEADER_ID, 0);
        convoyLead.setForceId(0);
        convoyLead.setLanceRole(LanceRole.convoy(OffBoardDirection.NORTH));
        Tank escort = new Tank();
        escort.setId(11);
        escort.setOwner(bot);
        escort.setMovementMode(EntityMovementMode.WHEELED);
        escort.setOriginalWalkMP(5);
        escort.setWeight(40);
        escort.setForceId(1);
        game.addEntity(escort);
        escort.setLanceRole(LanceRole.defaultEscort(0));

        List<Coords> kept = princess.getUnitOrdersFollower().keepReachable(escort, List.of(WEST_BANK, EAST_BANK));

        assertEquals(List.of(EAST_BANK), kept);
    }

    @Test
    void aMemberFirstDownTakesItsSlotRoundTheHexPickedForItsLeader() {
        Tank leader = truck(LEADER_ID, 0);
        Tank second = truck(4, 1);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        assertEquals(Optional.of(leader), follower.leaderStillToPlace(second));
        follower.setDeploymentAnchor(leader, EAST_BANK, second);

        assertTrue(follower.leaderStillToPlace(second).isEmpty(), "the hex is picked once");
        assertEquals(Optional.of(EAST_BANK), follower.getDeploymentAnchor(leader));
        Coords slot = follower.getDeploymentSlot(second, List.of(EAST_BANK, EAST_BANK.translated(3),
              EAST_BANK.translated(0), WEST_BANK)).orElseThrow();
        assertTrue(slot.distance(EAST_BANK) <= 2, "the slot " + slot + " is beside the hex picked for the leader");
    }

    private Tank lanceTruck(int unitId, int lanceId) {
        Tank truck = new Tank();
        truck.setId(unitId);
        truck.setOwner(bot);
        truck.setMovementMode(EntityMovementMode.WHEELED);
        truck.setOriginalWalkMP(4);
        truck.setWeight(35);
        truck.setForceId(lanceId);
        game.addEntity(truck);
        return truck;
    }

    private BipedMek lanceMek(int unitId, int lanceId) {
        BipedMek mek = new BipedMek();
        mek.setId(unitId);
        mek.setOwner(bot);
        mek.setForceId(lanceId);
        game.addEntity(mek);
        return mek;
    }

    @Test
    void aTruckWithNoOrdersDeploysOnItsLancesSideOfTheRiver() {
        Tank first = lanceTruck(3, 0);
        first.setPosition(EAST_BANK);
        first.setDeployed(true);
        Tank second = lanceTruck(4, 0);

        List<Coords> kept = princess.getUnitOrdersFollower().keepReachable(second,
              List.of(WEST_BANK, WEST_BANK.translated(0), EAST_BANK.translated(0), EAST_BANK.translated(3)));

        assertEquals(List.of(EAST_BANK.translated(0), EAST_BANK.translated(3)), kept);
    }

    @Test
    void aLanceThatCanAllWadeDeploysAsBefore() {
        BipedMek first = lanceMek(3, 0);
        first.setPosition(EAST_BANK);
        first.setDeployed(true);
        BipedMek second = lanceMek(4, 0);
        List<Coords> hexes = List.of(WEST_BANK, EAST_BANK.translated(0));

        assertEquals(hexes, princess.getUnitOrdersFollower().keepReachable(second, hexes));
    }

    @Test
    void anotherLancesUnitsDoNotHoldATruckBack() {
        Tank otherLance = lanceTruck(3, 0);
        otherLance.setPosition(EAST_BANK);
        otherLance.setDeployed(true);
        Tank truck = lanceTruck(4, 1);
        List<Coords> hexes = List.of(WEST_BANK, EAST_BANK.translated(0));

        assertEquals(hexes, princess.getUnitOrdersFollower().keepReachable(truck, hexes));
    }

    @Test
    void theFirstOfALanceDownKeepsHexesItsTrucksCanDriveTo() {
        BipedMek mek = lanceMek(3, 0);
        lanceTruck(4, 0);
        // the middle of the river: the Mek could stand there, and the truck can still drive up beside it
        Coords inRiver = new Coords(RIVER_COLUMN, 10);
        // a hex walled in by water a truck cannot get near
        Coords pond = new Coords(15, 10);
        for (Coords ring : pond.allAtDistanceOrLess(3)) {
            if (!ring.equals(pond)) {
                game.getBoard().getHex(ring).addTerrain(new Terrain(Terrains.WATER, 1));
            }
        }

        // each bank has room for the lance; the pond has room for one
        Coords westNext = WEST_BANK.translated(0);
        Coords eastNext = EAST_BANK.translated(0);
        List<Coords> kept = princess.getUnitOrdersFollower().keepReachable(mek,
              List.of(WEST_BANK, westNext, inRiver, pond, EAST_BANK, eastNext));

        assertEquals(List.of(WEST_BANK, westNext, inRiver, EAST_BANK, eastNext), kept);
    }
}
