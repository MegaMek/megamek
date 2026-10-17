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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import megamek.common.Hex;
import megamek.common.OffBoardDirection;
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
import megamek.common.orders.LanceRole;
import megamek.common.orders.UnitOrders;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests where escorts keep round their convoy: places aimed at the convoy's next waypoint, round where it will be when
 * it has still to move, and what an escort does once its convoy is gone (HammerGS, 2026-10-02).
 */
class EscortFollowerTest {

    private static final int WIDTH = 30;
    private static final int HEIGHT = 40;
    private static final int NORTH = 0;
    private static final int SOUTH_EAST = 2;
    private static final int CONVOY_FORCE_ID = 0;
    private static final int ESCORT_FORCE_ID = 1;
    private static final int CONVOY_HEAD_ID = 3;
    private static final int TRUCK_WALK_MP = 3;
    private static final Coords HEAD_HEX = new Coords(14, 30);
    private static final Coords NORTH_WAYPOINT = new Coords(14, 5);
    private static final int MEDIUM_DISTANCE = 5;

    private Game game;
    private Board board;
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
        board = new Board(WIDTH, HEIGHT, hexes);
        game = new Game();
        game.setBoard(board);
        bot = new Player(1, "Lyran Allies");
        bot.setBot(true);
        game.addPlayer(1, bot);
        princess = spy(new Princess("Lyran Allies", UUID.randomUUID().toString(), 1));
        doReturn(game).when(princess).getGame();
        game.setPhase(GamePhase.FIRING);
    }

    /** A unit whose walking movement a test can set, without a slow Mockito spy. */
    private static final class TestMek extends BipedMek {
        private int walkMP = TRUCK_WALK_MP;

        @Override
        public int getWalkMP() {
            return walkMP;
        }
    }

    private Entity unit(int unitId, int forceId, Coords position) {
        TestMek mek = new TestMek();
        mek.setId(unitId);
        mek.setOwner(bot);
        mek.setForceId(forceId);
        game.addEntity(mek);
        mek.setPosition(position);
        mek.setDeployed(true);
        return mek;
    }

    /** A three-truck Column heading north for its waypoint, its head in front. */
    private List<Entity> convoyHeadingNorth() {
        List<Entity> trucks = new ArrayList<>();
        for (int slot = 0; slot < 3; slot++) {
            Entity truck = unit(CONVOY_HEAD_ID + slot, CONVOY_FORCE_ID, HEAD_HEX.translated(3, slot));
            truck.setLanceRole(LanceRole.convoy(OffBoardDirection.NORTH));
            truck.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)).withFormation(
                  new FormationOrder(FormationShape.COLUMN, CONVOY_HEAD_ID, 1, slot, FormationPace.RUN,
                        ContactRule.HOLD)));
            truck.setDone(true);
            trucks.add(truck);
        }
        return trucks;
    }

    private Entity escort(int unitId, Coords position, LanceRole role) {
        Entity mek = unit(unitId, ESCORT_FORCE_ID, position);
        mek.setLanceRole(role);
        return mek;
    }

    private static LanceRole surround() {
        return LanceRole.escort(CONVOY_FORCE_ID, EnumSet.allOf(LanceRole.Position.class), LanceRole.Distance.MEDIUM,
              LanceRole.Movement.IN_STEP, LanceRole.Contact.SCREEN, LanceRole.LeaveToFight.BRIEFLY,
              LanceRole.WhenConvoyGone.FOLLOW);
    }

    @Test
    void theLeadKeepsAheadOfTheHeadTowardTheConvoysNextWaypoint() {
        convoyHeadingNorth();
        // the escort nearest the front takes Lead
        Entity lead = escort(10, HEAD_HEX.translated(NORTH, 2), surround());
        escort(11, HEAD_HEX.translated(3, 8), surround());

        assertEquals(Optional.of(HEAD_HEX.translated(NORTH, MEDIUM_DISTANCE)),
              princess.getUnitOrdersFollower().getEscortPlace(lead));
    }

    @Test
    void theRearKeepsBehindTheTailAndTheFlanksBesideTheMiddle() {
        List<Entity> trucks = convoyHeadingNorth();
        Entity lead = escort(10, HEAD_HEX.translated(NORTH, 2), surround());
        Entity left = escort(11, HEAD_HEX.translated(5, 4), surround());
        Entity right = escort(12, HEAD_HEX.translated(1, 4), surround());
        Entity rear = escort(13, HEAD_HEX.translated(3, 6), surround());
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        Coords tail = trucks.get(2).getPosition();
        Coords middle = trucks.get(1).getPosition();
        assertEquals(Optional.of(tail.translated(3, MEDIUM_DISTANCE)), follower.getEscortPlace(rear));
        Coords leftPlace = follower.getEscortPlace(left).orElseThrow();
        Coords rightPlace = follower.getEscortPlace(right).orElseThrow();
        assertEquals(MEDIUM_DISTANCE, middle.distance(leftPlace));
        assertEquals(MEDIUM_DISTANCE, middle.distance(rightPlace));
        assertTrue(leftPlace.getX() < middle.getX(), "left of a convoy heading north is west");
        assertTrue(rightPlace.getX() > middle.getX(), "right of a convoy heading north is east");
        assertTrue(follower.getEscortPlace(lead).isPresent());
    }

    @Test
    void thePlacesTurnWithTheConvoyAtItsNextWaypoint() {
        List<Entity> trucks = convoyHeadingNorth();
        Coords eastWaypoint = HEAD_HEX.translated(SOUTH_EAST, 10);
        for (Entity truck : trucks) {
            truck.setUnitOrders(truck.getUnitOrders().withRoute(List.of(eastWaypoint)));
        }
        Entity lead = escort(10, HEAD_HEX.translated(SOUTH_EAST, 2), LanceRole.defaultEscort(CONVOY_FORCE_ID));

        assertEquals(Optional.of(HEAD_HEX.translated(SOUTH_EAST, MEDIUM_DISTANCE)),
              princess.getUnitOrdersFollower().getEscortPlace(lead));
    }

    @Test
    void aConvoyStillToMoveIsMetWhereItWillBe() {
        List<Entity> trucks = convoyHeadingNorth();
        game.setPhase(GamePhase.MOVEMENT);
        trucks.get(0).setDone(false);
        Entity lead = escort(10, HEAD_HEX.translated(NORTH, 2), LanceRole.defaultEscort(CONVOY_FORCE_ID));

        // the head will be a walk on toward its waypoint, and Lead keeps five hexes ahead of that
        assertEquals(Optional.of(HEAD_HEX.translated(NORTH, TRUCK_WALK_MP + MEDIUM_DISTANCE)),
              princess.getUnitOrdersFollower().getEscortPlace(lead));
    }

    @Test
    void anEscortKeepsItsPlaceTurnToTurn() {
        convoyHeadingNorth();
        Entity first = escort(10, HEAD_HEX.translated(5, 4), LanceRole.defaultEscort(CONVOY_FORCE_ID));
        Entity second = escort(11, HEAD_HEX.translated(1, 4), LanceRole.defaultEscort(CONVOY_FORCE_ID));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        Coords firstPlace = follower.getEscortPlace(first).orElseThrow();
        Coords secondPlace = follower.getEscortPlace(second).orElseThrow();

        // the two cross over on the way; each keeps its own place rather than swapping
        first.setPosition(HEAD_HEX.translated(1, 3));
        second.setPosition(HEAD_HEX.translated(5, 3));
        game.setCurrentRound(game.getCurrentRound() + 1);

        assertEquals(Optional.of(firstPlace), follower.getEscortPlace(first));
        assertEquals(Optional.of(secondPlace), follower.getEscortPlace(second));
    }

    @Test
    void aConvoyDestroyedLeavesAFollowingEscortHoldingWhereItFell() {
        List<Entity> trucks = convoyHeadingNorth();
        Entity lead = escort(10, HEAD_HEX.translated(NORTH, 2), LanceRole.defaultEscort(CONVOY_FORCE_ID));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        follower.getEscortPlace(lead);
        Coords middle = trucks.get(1).getPosition();

        for (Entity truck : trucks) {
            truck.setDestroyed(true);
        }

        assertEquals(Optional.of(middle), follower.getEscortPlace(lead));
        assertTrue(follower.getOrderedEdge(lead).isEmpty());
    }

    @Test
    void anEscortSetToBreakOffFightsAsALanceOnceTheConvoyIsGone() {
        List<Entity> trucks = convoyHeadingNorth();
        LanceRole breakOff = LanceRole.escort(CONVOY_FORCE_ID, EnumSet.of(LanceRole.Position.LEAD),
              LanceRole.Distance.MEDIUM, LanceRole.Movement.IN_STEP, LanceRole.Contact.SCREEN,
              LanceRole.LeaveToFight.BRIEFLY, LanceRole.WhenConvoyGone.BREAK_OFF);
        Entity lead = escort(10, HEAD_HEX.translated(NORTH, 2), breakOff);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        follower.getEscortPlace(lead);

        for (Entity truck : trucks) {
            truck.setDestroyed(true);
        }

        assertTrue(follower.getEscortPlace(lead).isEmpty());
    }

    @Test
    void aWaypointCloseByDoesNotSwingThePlaces() {
        List<Entity> trucks = convoyHeadingNorth();
        // two hexes off to the south-east, then on north: the convoy is aimed past the close one
        Coords closeWaypoint = HEAD_HEX.translated(SOUTH_EAST, 2);
        for (Entity truck : trucks) {
            truck.setUnitOrders(truck.getUnitOrders().withRoute(List.of(closeWaypoint, NORTH_WAYPOINT)));
        }
        Entity lead = escort(10, HEAD_HEX.translated(NORTH, 2), LanceRole.defaultEscort(CONVOY_FORCE_ID));

        Coords leadPlace = princess.getUnitOrdersFollower().getEscortPlace(lead).orElseThrow();

        assertEquals(HEAD_HEX.direction(NORTH_WAYPOINT), HEAD_HEX.direction(leadPlace));
    }

    @Test
    void theConvoyTurnsOneHexSideARound() {
        List<Entity> trucks = convoyHeadingNorth();
        Entity lead = escort(10, HEAD_HEX.translated(NORTH, 2), LanceRole.defaultEscort(CONVOY_FORCE_ID));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        follower.getEscortPlace(lead);

        // the route now runs south-east, two hex sides round from north
        Coords southEastWaypoint = HEAD_HEX.translated(SOUTH_EAST, 10);
        for (Entity truck : trucks) {
            truck.setUnitOrders(truck.getUnitOrders().withRoute(List.of(southEastWaypoint)));
        }
        game.setCurrentRound(game.getCurrentRound() + 1);
        assertEquals(Optional.of(HEAD_HEX.translated(1, MEDIUM_DISTANCE)), follower.getEscortPlace(lead));

        game.setCurrentRound(game.getCurrentRound() + 1);
        assertEquals(Optional.of(HEAD_HEX.translated(SOUTH_EAST, MEDIUM_DISTANCE)), follower.getEscortPlace(lead));
    }

    @Test
    void aPlacePastTheExitEdgeStandsOnTheEdge() {
        List<Entity> trucks = convoyHeadingNorth();
        Coords nearTheEdge = new Coords(14, 2);
        trucks.get(0).setPosition(nearTheEdge);
        for (Entity truck : trucks) {
            truck.setUnitOrders(truck.getUnitOrders().withRoute(List.of()));
        }
        Entity lead = escort(10, new Coords(14, 6), LanceRole.defaultEscort(CONVOY_FORCE_ID));

        Coords leadPlace = princess.getUnitOrdersFollower().getEscortPlace(lead).orElseThrow();

        assertTrue(board.contains(leadPlace), "the Lead place " + leadPlace + " is on the board");
        assertEquals(0, leadPlace.getY());
    }

    @Test
    void anEscortFacesTheWayTheConvoyIsGoingWithItsLegs() {
        List<Entity> trucks = convoyHeadingNorth();
        Coords eastWaypoint = HEAD_HEX.translated(SOUTH_EAST, 10);
        for (Entity truck : trucks) {
            truck.setUnitOrders(truck.getUnitOrders().withRoute(List.of(eastWaypoint)));
        }
        Entity right = escort(10, HEAD_HEX.translated(1, 4), LanceRole.defaultEscort(CONVOY_FORCE_ID));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        Coords place = follower.getEscortPlace(right).orElseThrow();

        assertEquals(SOUTH_EAST, follower.orderedFacing(right, place));
        assertEquals(0, follower.twistAllowance(right, place));
        // an enemy off to its side leaves the convoy's facing as it is
        assertEquals(SOUTH_EAST, follower.facingThatStandsFor(right, SOUTH_EAST, place, place.translated(NORTH, 4)));
    }

    @Test
    void aConvoyFacesItsExitEdge() {
        Coords middle = new Coords(10, 17);
        Entity truck = unit(3, CONVOY_FORCE_ID, middle);

        assertTrue(ConvoyTracker.exitFacing(truck, middle, board).isEmpty());

        truck.setLanceRole(LanceRole.convoy(OffBoardDirection.NORTH));
        assertEquals(0, ConvoyTracker.exitFacing(truck, middle, board).getAsInt());
        truck.setLanceRole(LanceRole.convoy(OffBoardDirection.SOUTH));
        assertEquals(3, ConvoyTracker.exitFacing(truck, middle, board).getAsInt());
        // standing on the north edge already, it faces straight off it
        truck.setLanceRole(LanceRole.convoy(OffBoardDirection.NORTH));
        assertEquals(0, ConvoyTracker.exitFacing(truck, new Coords(10, 0), board).getAsInt());
    }

    @Test
    void escortsFillEveryPlaceBeforeDoublingUp() {
        Map<Integer, Coords> escorts = new HashMap<>();
        escorts.put(1, new Coords(5, 5));
        escorts.put(2, new Coords(6, 5));
        escorts.put(3, new Coords(7, 5));
        escorts.put(4, new Coords(8, 5));
        List<LanceRole.Position> positions = List.of(LanceRole.Position.LEAD, LanceRole.Position.LEFT,
              LanceRole.Position.RIGHT);
        Map<LanceRole.Position, Coords> places = Map.of(LanceRole.Position.LEAD, new Coords(5, 0),
              LanceRole.Position.LEFT, new Coords(0, 5), LanceRole.Position.RIGHT, new Coords(10, 5));

        Map<Integer, LanceRole.Position> assigned = EscortPlanner.assign(escorts, positions, places, Map.of());

        assertEquals(4, assigned.size());
        for (LanceRole.Position position : positions) {
            assertTrue(assigned.containsValue(position), position + " has an escort");
        }
    }

    private static final int EXTERNAL_FORCE_ID = 9;
    private static final int ESCORT_REACH = 15;

    private static LanceRole leadEscort(LanceRole.Contact contact, LanceRole.LeaveToFight leave) {
        return LanceRole.escort(CONVOY_FORCE_ID, EnumSet.of(LanceRole.Position.LEAD), LanceRole.Distance.MEDIUM,
              LanceRole.Movement.IN_STEP, contact, leave, LanceRole.WhenConvoyGone.FOLLOW);
    }

    /** An enemy the escorts can see, the given hexes east of the convoy's head. */
    private Entity enemyEastOfTheHead(int hexes) {
        Entity enemy = unit(40, EXTERNAL_FORCE_ID, HEAD_HEX.translated(SOUTH_EAST, hexes));
        doReturn(List.of(enemy)).when(princess).getEnemyEntities();
        doReturn(ESCORT_REACH).when(princess).getMaxWeaponRange(any(Entity.class));
        return enemy;
    }

    @Test
    void onContactAScreeningEscortGetsBetweenTheThreatAndTheConvoy() {
        // HammerGS, 2026-10-02: Screen gets between the threat and the convoy
        convoyHeadingNorth();
        Entity lead = escort(10, HEAD_HEX.translated(NORTH, 2),
              leadEscort(LanceRole.Contact.SCREEN, LanceRole.LeaveToFight.BRIEFLY));
        Entity enemy = enemyEastOfTheHead(8);

        Coords place = princess.getUnitOrdersFollower().getEscortPlace(lead).orElseThrow();

        assertEquals(ConvoyTracker.stepToward(HEAD_HEX, enemy.getPosition(), MEDIUM_DISTANCE), place);
    }

    @Test
    void onContactAnEscortSetToStayKeepsItsPlace() {
        convoyHeadingNorth();
        Entity lead = escort(10, HEAD_HEX.translated(NORTH, 2),
              leadEscort(LanceRole.Contact.STAY, LanceRole.LeaveToFight.HUNT));
        enemyEastOfTheHead(8);

        assertEquals(Optional.of(HEAD_HEX.translated(NORTH, MEDIUM_DISTANCE)),
              princess.getUnitOrdersFollower().getEscortPlace(lead));
    }

    @Test
    void anEscortBreakingOffBrieflyClosesNoMoreThanFourHexes() {
        convoyHeadingNorth();
        Entity lead = escort(10, HEAD_HEX.translated(NORTH, 2),
              leadEscort(LanceRole.Contact.BREAK_AND_FIGHT, LanceRole.LeaveToFight.BRIEFLY));
        Entity enemy = enemyEastOfTheHead(12);
        Coords normalPlace = HEAD_HEX.translated(NORTH, MEDIUM_DISTANCE);

        Coords place = princess.getUnitOrdersFollower().getEscortPlace(lead).orElseThrow();

        assertEquals(LanceRole.BRIEF_CHASE_HEXES, normalPlace.distance(place));
        assertTrue(place.distance(enemy.getPosition()) < normalPlace.distance(enemy.getPosition()));
    }

    @Test
    void anEscortSetToHuntLeavesItsPlaceUntilTheThreatIsGone() {
        convoyHeadingNorth();
        Entity lead = escort(10, HEAD_HEX.translated(NORTH, 2),
              leadEscort(LanceRole.Contact.BREAK_AND_FIGHT, LanceRole.LeaveToFight.HUNT));
        Entity enemy = enemyEastOfTheHead(8);

        assertTrue(princess.getUnitOrdersFollower().getEscortPlace(lead).isEmpty(), "fights as a lance");

        enemy.setPosition(HEAD_HEX.translated(SOUTH_EAST, ESCORT_REACH + 6));
        game.setTurnIndex(game.getTurnIndex() + 1, bot.getId());

        assertEquals(Optional.of(HEAD_HEX.translated(NORTH, MEDIUM_DISTANCE)),
              princess.getUnitOrdersFollower().getEscortPlace(lead), "back to its place");
    }
}
