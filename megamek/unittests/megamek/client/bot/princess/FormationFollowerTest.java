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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.orders.ContactRule;
import megamek.common.orders.EdgeOrder;
import megamek.common.orders.FightState;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointFormation;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the formation half of {@link UnitOrdersFollower}, on a real board: where followers stand, how they get
 * round blocked slots, who leads when the leader falls, when a formation breaks on contact, and the leader's pace.
 */
class FormationFollowerTest {

    private static final int WIDTH = 30;
    private static final int HEIGHT = 30;
    private static final int CLIFF_LEVEL = 5;
    private static final int NORTH = 0;
    private static final int NORTH_EAST = 1;
    private static final int SOUTH_EAST = 2;
    private static final int SOUTH = 3;
    private static final int SOUTH_WEST = 4;
    private static final int NORTH_WEST = 5;
    private static final Coords LEADER_HEX = new Coords(14, 20);
    private static final Coords NORTH_WAYPOINT = new Coords(14, 2);

    private Game game;
    private Board board;
    private Player bot;
    private Princess princess;
    private final List<Entity> enemies = new ArrayList<>();

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
        doReturn(enemies).when(princess).getEnemyEntities();
    }

    /**
     * A Mek whose movement, armor and state a test can set. A Mockito spy did the same, but made every call on the
     * unit slow, and the route fields ask the unit thousands of questions: the formation tests took minutes each.
     */
    private static final class TestMek extends BipedMek {
        private int walkMP;
        private Integer runMP;
        private Integer totalArmor;
        private Double weight;
        private boolean immobile;
        private boolean prone;

        @Override
        public int getWalkMP() {
            return walkMP;
        }

        @Override
        public int getRunMP() {
            return (runMP == null) ? super.getRunMP() : runMP;
        }

        @Override
        public int getTotalArmor() {
            return (totalArmor == null) ? super.getTotalArmor() : totalArmor;
        }

        @Override
        public double getWeight() {
            return (weight == null) ? super.getWeight() : weight;
        }

        @Override
        public boolean isImmobile() {
            return immobile || super.isImmobile();
        }

        @Override
        public boolean isProne() {
            return prone || super.isProne();
        }
    }

    private static TestMek fake(BipedMek mek) {
        return (TestMek) mek;
    }

    private BipedMek member(int unitId, Coords position, int slot, int walkMP) {
        TestMek mek = new TestMek();
        mek.setId(unitId);
        mek.setOwner(bot);
        game.addEntity(mek);
        mek.setPosition(position);
        mek.walkMP = walkMP;
        mek.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)).withFormation(
              new FormationOrder(FormationShape.ECHELON_RIGHT, 20, 2, slot, FormationPace.WALK, ContactRule.BREAK)));
        return mek;
    }

    @Test
    void aFollowersTargetIsItsSlotAroundTheLeadersWaypoint() {
        member(20, NORTH_WAYPOINT, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);

        // heading north to the waypoint, the Echelon Right forms there, stepping back south-east
        assertEquals(Optional.of(NORTH_WAYPOINT.translated(SOUTH_EAST, 2)),
              princess.getUnitOrdersFollower().getFormationSlot(second));
    }

    @Test
    void aUnitLeavingTheFormationLetsTheOthersCloseUp() {
        // HammerGS: detach a unit, and the rest re-slot to close the gap
        member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        BipedMek third = member(22, new Coords(17, 25), 2, 4);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        Coords secondPlace = follower.getFormationSlot(second).orElseThrow();
        assertNotEquals(secondPlace, follower.getFormationSlot(third).orElseThrow());

        second.setUnitOrders(second.getUnitOrders().withFormation(null));

        assertEquals(Optional.of(secondPlace), follower.getFormationSlot(third));
    }

    @Test
    void aBotUnitFollowsAPlayersUnitWithoutARouteOfItsOwn() {
        // HammerGS: follow a player's unit - the bot's units form up on it wherever it goes
        Player human = new Player(2, "Lyran Allies Commander");
        human.setTeam(1);
        bot.setTeam(1);
        game.addPlayer(2, human);
        BipedMek playerMek = new BipedMek();
        playerMek.setId(30);
        playerMek.setOwner(human);
        game.addEntity(playerMek);
        playerMek.setPosition(LEADER_HEX);
        playerMek.setFacing(0);
        BipedMek escort = loneUnit(31, new Coords(10, 10), UnitOrders.NONE.withFormation(
              new FormationOrder(FormationShape.ECHELON_RIGHT, 30, 2, 1, FormationPace.WALK, ContactRule.BREAK)));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        assertTrue(follower.isFollowingPlayerUnit(escort));
        // facing north, the Echelon Right steps back south-east from the player's unit
        assertEquals(Optional.of(LEADER_HEX.translated(SOUTH_EAST, 2)), follower.getFormationSlot(escort));
        assertEquals(Optional.of(LEADER_HEX.translated(SOUTH_EAST, 2)),
              princess.getUnitBehaviorTracker().getActiveWaypoint(escort, princess));

        playerMek.setPosition(NORTH_WAYPOINT);
        assertEquals(Optional.of(NORTH_WAYPOINT.translated(SOUTH_EAST, 2)), follower.getFormationSlot(escort));
    }

    @Test
    void callingOffAFleeKeepsTheLanceInFormation() {
        // cancelling a flee ends the exit order only; the unit's formation, facings and priority stay
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
        leader.setUnitOrders(leader.getUnitOrders().withFacings(2, 3)
              .withEdgeOrder(EdgeOrder.EXIT_BY, OffBoardDirection.NORTH));
        doReturn(List.<Entity>of(leader)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());

        princess.getUnitOrdersFollower().cancelExitOrders();

        assertEquals(EdgeOrder.NONE, leader.getUnitOrders().getEdgeOrder());
        assertTrue(leader.getUnitOrders().getFormation().isPresent());
        assertEquals(3, leader.getUnitOrders().getFacingWhenStopped());
    }

    @Test
    void aBotUnitThatLeftItsFormationDoesNotLeadItAgain() {
        // only a player's unit leads without a formation order; a bot leader that left hands over to the next unit
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        leader.setUnitOrders(leader.getUnitOrders().withFormation(null));

        assertFalse(princess.getUnitOrdersFollower().isFollowingPlayerUnit(second));
        assertEquals(Optional.empty(), princess.getUnitOrdersFollower().getFormationSlot(second));
    }

    @Test
    void aUnitNeverRunsAheadOfALeaderStillOnItsWay() {
        // HammerGS's playtest: with slots laid out around the next flag, a Centurion ran two hexes in front of the
        // Grasshopper leading it. On the way the shape sits around the leader; at the flag it forms there.
        game.setPhase(GamePhase.MOVEMENT);
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);

        // heading north, the Echelon Right steps back south-east of the leader itself
        assertEquals(Optional.of(LEADER_HEX.translated(SOUTH_EAST, 2)),
              princess.getUnitOrdersFollower().getFormationSlot(second));

        leader.setPosition(NORTH_WAYPOINT);
        assertEquals(Optional.of(NORTH_WAYPOINT.translated(SOUTH_EAST, 2)),
              princess.getUnitOrdersFollower().getFormationSlot(second));
    }

    @Test
    void aUnitStaysBesideItsLeaderTurnAfterTurnNotJustTheFirst() {
        // HammerGS's playtest: only the first look at the slot kept the unit beside its moving leader; every look
        // after it sent the Griffin on to its slot at the next flag, four rounds ahead of its Stalker
        game.setPhase(GamePhase.MOVEMENT);
        member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        Optional<Coords> firstLook = follower.getFormationSlot(second);
        assertEquals(Optional.of(LEADER_HEX.translated(SOUTH_EAST, 2)), firstLook);
        assertEquals(firstLook, follower.getFormationSlot(second));
    }

    @Test
    void aUnitOnItsWayFacesTheWayRoundAnObstacleNotStraightAtTheFlag() {
        // HammerGS's playtest: facing the flag straight through a building block, a 3 MP Stalker could not step round
        // the corner and turn back to the flag in one move, and rocked between two hexes for ten rounds
        BipedMek scout = loneUnit(30, LEADER_HEX, UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)));
        Coords blocked = LEADER_HEX.translated(NORTH, 1);
        board.getHex(blocked).setLevel(CLIFF_LEVEL);
        board.getHex(blocked).addTerrain(new Terrain(Terrains.IMPASSABLE, 1));

        int facing = princess.getUnitOrdersFollower().orderedFacing(scout, LEADER_HEX);

        assertTrue((facing == NORTH_EAST) || (facing == NORTH_WEST), "faced " + facing);
    }

    private void buildStreetNorthOfTheLeader() {
        for (int y = 6; y <= 16; y++) {
            board.getHex(LEADER_HEX.getX() - 1, y).addTerrain(new Terrain(Terrains.BUILDING, 1));
            board.getHex(LEADER_HEX.getX() + 1, y).addTerrain(new Terrain(Terrains.BUILDING, 1));
        }
    }

    @Test
    void aLanceGoingThroughATownBreaksFormationAndEachUnitMakesForItsPlaceAtTheFlag() {
        // HammerGS's town walk (2026-10-01): through the streets the line came apart and formed again at the far
        // side; out on open ground the same lance keeps its places beside its leader (see the test above)
        game.setPhase(GamePhase.MOVEMENT);
        buildStreetNorthOfTheLeader();
        member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        assertEquals(Optional.of(NORTH_WAYPOINT.translated(SOUTH_EAST, 2)), follower.getFormationSlot(second));
        assertTrue(follower.isOnTownLeg(second));
    }

    @Test
    void throughATownTheFirstUnitThroughTakesTheNearestPlace() {
        // HammerGS's town walk: the places were not fixed at the start; the Griffin, through first, took the nearest
        // end spot and the Stalker coming round the long way took what was left (2026-10-01)
        game.setPhase(GamePhase.MOVEMENT);
        buildStreetNorthOfTheLeader();
        member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        BipedMek third = member(22, new Coords(12, 25), 2, 4);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        game.setCurrentRound(3);
        Coords secondPlace = follower.getFormationSlot(second).orElseThrow();
        Coords thirdPlace = follower.getFormationSlot(third).orElseThrow();

        // each finds itself next to the other's place
        second.setPosition(thirdPlace.translated(SOUTH, 1));
        third.setPosition(secondPlace.translated(SOUTH, 1));
        game.setCurrentRound(4);

        assertEquals(Optional.of(thirdPlace), follower.getFormationSlot(second));
        assertEquals(Optional.of(secondPlace), follower.getFormationSlot(third));
    }

    @Test
    void aUnitGoingRoundAnObstacleKeepsTheFlagInItsFrontArc() {
        // HammerGS: each Mek faced "a mix of both" - the way it was going next, and the objective (2026-10-01)
        Coords flagNorthEast = LEADER_HEX.translated(NORTH_EAST, 6);
        BipedMek scout = loneUnit(30, LEADER_HEX, UnitOrders.NONE.withRoute(List.of(flagNorthEast)));
        for (int direction : new int[] { NORTH, NORTH_EAST, SOUTH_EAST }) {
            Coords blocked = LEADER_HEX.translated(direction, 1);
            board.getHex(blocked).setLevel(CLIFF_LEVEL);
            board.getHex(blocked).addTerrain(new Terrain(Terrains.IMPASSABLE, 1));
        }

        // the way round leaves two or more hexsides off the flag; the unit turns one hexside toward it, no further
        int facing = princess.getUnitOrdersFollower().orderedFacing(scout, LEADER_HEX);
        int towardFlag = LEADER_HEX.direction(flagNorthEast);
        assertEquals(1, UnitOrdersFollower.sidesApart(facing, towardFlag), "faced " + facing);
    }

    @Test
    void clearingAWayThroughBuildingsIsOnlyForWhenNoWayOnFootExists() {
        // HammerGS: "the bulldozer plan should be a last resort when no walk path exists" (2026-10-01)
        BipedMek scout = loneUnit(30, LEADER_HEX, UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        game.setPhase(GamePhase.MOVEMENT);

        assertTrue(follower.hasWalkingRoute(scout));

        for (int direction = 0; direction < 6; direction++) {
            Coords wall = NORTH_WAYPOINT.translated(direction);
            board.getHex(wall).setLevel(CLIFF_LEVEL);
            board.getHex(wall).addTerrain(new Terrain(Terrains.IMPASSABLE, 1));
        }
        game.setCurrentRound(game.getCurrentRound() + 1);

        assertFalse(follower.hasWalkingRoute(scout));
    }

    @Test
    void atAWaypointPartWayTheShapeFacesTheNextLeg() {
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        BipedMek leader = member(20, NORTH_WAYPOINT, 0, 3);
        leader.setUnitOrders(leader.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT, eastWaypoint)));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);

        // facing south-east for the next leg, Echelon Right steps back two facings round, to the south-west
        assertEquals(Optional.of(NORTH_WAYPOINT.translated(SOUTH_WEST, 2)),
              princess.getUnitOrdersFollower().getFormationSlot(second));
    }

    @Test
    void aFollowerPassingNearAWaypointDoesNotRunAheadOfItsLeader() {
        // HammerGS's playtest: a Firestarter starting near the first waypoint ticked it off its own route, then
        // headed for the second waypoint while the rest of the Wedge formed on the first.
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
        Coords secondWaypoint = NORTH_WAYPOINT.translated(SOUTH_WEST, 6);
        leader.setUnitOrders(leader.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT, secondWaypoint)));
        BipedMek second = member(21, NORTH_WAYPOINT.translated(SOUTH, 1), 1, 4);
        second.setUnitOrders(second.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT, secondWaypoint)));
        doReturn(List.of(leader, second)).when(princess).getEntitiesOwned();

        princess.getUnitOrdersFollower().advanceRoutes();

        assertEquals(List.of(NORTH_WAYPOINT, secondWaypoint), second.getUnitOrders().getRoute());
    }

    @Test
    void theLeaderHasNoSlotAndFollowsItsRoute() {
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
        member(21, new Coords(16, 25), 1, 4);

        assertTrue(princess.getUnitOrdersFollower().getFormationSlot(leader).isEmpty());
    }

    @Test
    void aBlockedSlotTakesTheBestHexNextToIt() {
        member(20, NORTH_WAYPOINT, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        Coords ideal = NORTH_WAYPOINT.translated(SOUTH_EAST, 2);
        board.getHex(ideal).setLevel(CLIFF_LEVEL);
        board.getHex(ideal).addTerrain(new Terrain(Terrains.IMPASSABLE,
              1));

        Coords slot = princess.getUnitOrdersFollower().getFormationSlot(second).orElseThrow();

        assertEquals(1, slot.distance(ideal));
    }

    @Test
    void aFormationThatCannotFitFoldsIntoAColumn() {
        member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        // wall off everything east of the waypoint, leaving the column behind it open
        for (int x = NORTH_WAYPOINT.getX() + 1; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                board.getHex(x, y).addTerrain(new Terrain(
                      Terrains.IMPASSABLE, 1));
            }
        }

        assertEquals(Optional.of(NORTH_WAYPOINT.translated(SOUTH, 2)),
              princess.getUnitOrdersFollower().getFormationSlot(second));
    }

    @Test
    void theNextUnitLeadsWhenTheLeaderIsGoneAndThereIsNoSecondInCommand() {
        // with a second-in-command (place 3) it takes over instead: see theSecondInCommandTakesCommandWhenTheCommander
        // IsLost
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        leader.setDestroyed(true);

        assertTrue(princess.getUnitOrdersFollower().getFormationSlot(second).isEmpty());
    }

    @Test
    void anEnemyInRangeThatHasNotFiredDoesNotBreakTheLance() {
        // HammerGS: the trigger is being hit, not an enemy walking into range
        member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        Entity enemy = mock(Entity.class);
        when(enemy.getPosition()).thenReturn(LEADER_HEX.translated(0, 5));
        when(enemy.getBoardId()).thenReturn(0);
        enemies.add(enemy);

        assertTrue(princess.getUnitOrdersFollower().getFormationSlot(second).isPresent());
    }

    @Test
    void aLanceSetToBreakAndFightFightsWhenHitThenHoldsForOrders() {
        // QA's case: missile fire from the flank. Break and fight leaves the route to fight, then waits for Resume.
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        BipedMek leader = member(20, LEADER_HEX, 0, 5);
        leader.setUnitOrders(leader.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT, eastWaypoint)));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        second.setUnitOrders(second.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT, eastWaypoint)));
        doReturn(List.<Entity>of(leader, second)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
        fake(second).totalArmor = 40;
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        game.setCurrentRound(3);
        follower.advanceRoutes();
        assertTrue(leader.getUnitOrders().getFightState().isEmpty());

        // the Longbow's flank is hit by missiles in round 3's firing
        fake(second).totalArmor = 34;
        game.setCurrentRound(4);
        follower.advanceRoutes();
        assertEquals(Optional.of(FightState.FIGHTING), leader.getUnitOrders().getFightState());
        assertEquals(Optional.of(FightState.FIGHTING), second.getUnitOrders().getFightState());
        assertTrue(princess.getUnitBehaviorTracker().getActiveWaypoint(leader, princess).isEmpty());
        assertTrue(follower.getFormationSlot(second).isEmpty());

        // a full turn without a hit: the lance holds, keeping its route, until the Resume order
        game.setCurrentRound(5);
        follower.advanceRoutes();
        assertEquals(Optional.of(FightState.AWAITING_ORDERS), leader.getUnitOrders().getFightState());
        assertTrue(leader.getUnitOrders().isPaused());
        assertEquals(2, leader.getUnitOrders().getRoute().size());

        leader.setUnitOrders(UnitOrderAction.RESUME.apply(leader.getUnitOrders(), List.of(), OffBoardDirection.NONE,
              UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, 5));
        assertTrue(leader.getUnitOrders().getFightState().isEmpty());
        assertFalse(leader.getUnitOrders().isPaused());
    }

    @Test
    void aLanceSetToTurnAndFireSquaresUpToItsAttackersOnlyOnceHit() {
        // Turn and fire: keep moving along the route, but turn to bring attackers into the front arc
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        List<WaypointOrder> turnAndFire = List.of(new WaypointOrder(UnitOrders.FACING_AUTO, 0,
              new WaypointFormation(FormationShape.ECHELON_RIGHT, 2, FormationPace.WALK, ContactRule.TURN_AND_FIRE,
                    false)), WaypointOrder.PASS_THROUGH);
        BipedMek leader = member(20, LEADER_HEX, 0, 5);
        leader.setUnitOrders(leader.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT, eastWaypoint), turnAndFire));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        doReturn(List.<Entity>of(leader, second)).when(princess).getEntitiesOwned();
        fake(leader).totalArmor = 40;
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        Coords attackerOnTheLeftFlank = LEADER_HEX.translated(SOUTH_WEST, 4);
        game.setCurrentRound(3);

        // not yet hit: the route facing holds
        assertEquals(NORTH, follower.facingThatStandsFor(leader, NORTH, LEADER_HEX, attackerOnTheLeftFlank));

        fake(leader).totalArmor = 33;
        game.setCurrentRound(4);
        assertEquals(UnitOrders.FACING_AUTO, follower.facingThatStandsFor(leader, NORTH, LEADER_HEX,
              attackerOnTheLeftFlank));
    }

    @Test
    void aLanceSetToPushThroughKeepsToItsRouteWhenHit() {
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        FormationOrder pushThrough = new FormationOrder(FormationShape.ECHELON_RIGHT, 20, 2, 0, FormationPace.WALK,
              ContactRule.HOLD);
        BipedMek leader = member(20, LEADER_HEX, 0, 5);
        leader.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, eastWaypoint))
              .withFormation(pushThrough));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        second.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, eastWaypoint)).withFormation(
              new FormationOrder(FormationShape.ECHELON_RIGHT, 20, 2, 1, FormationPace.WALK, ContactRule.HOLD)));
        doReturn(List.<Entity>of(leader, second)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
        fake(second).totalArmor = 40;
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        game.setCurrentRound(3);
        follower.advanceRoutes();

        fake(second).totalArmor = 34;
        game.setCurrentRound(4);
        follower.advanceRoutes();

        assertTrue(leader.getUnitOrders().getFightState().isEmpty());
        assertTrue(follower.getFormationSlot(second).isPresent());
    }

    private static MovePath moveUsing(int movementPoints) {
        MovePath path = mock(MovePath.class);
        when(path.getMpUsed()).thenReturn(movementPoints);
        // a real move always has its steps; the formation filter reads them for buildings it would bring down
        when(path.getStepVector()).thenReturn(new Vector<>());
        return path;
    }

    private static FormationOrder paced(int slot, FormationPace pace, ContactRule contactRule) {
        return new FormationOrder(FormationShape.WEDGE, 20, 2, slot, pace, contactRule);
    }

    @Test
    void atAWalkPaceEachUnitWalksUpToItsOwnWalkNotTheSlowestUnits() {
        // HammerGS's playtest: a Grasshopper walking 5 leading a Longbow walking 3 was held to three hexes a turn
        // while the rest of the Wedge waited in their slots for it. Each unit now walks at its own speed.
        BipedMek grasshopper = member(20, LEADER_HEX, 0, 5);
        BipedMek longbow = member(21, new Coords(16, 25), 1, 3);
        MovePath walkThree = moveUsing(3);
        MovePath jumpFive = moveUsing(5);
        MovePath runSeven = moveUsing(7);
        // in its place, the Longbow keeps to the walk
        longbow.setPosition(princess.getUnitOrdersFollower().getFormationSlot(longbow).orElseThrow());

        assertEquals(List.of(walkThree, jumpFive), princess.getUnitOrdersFollower().limitToFormationPace(grasshopper,
              List.of(walkThree, jumpFive, runSeven)));
        assertEquals(List.of(walkThree), princess.getUnitOrdersFollower().limitToFormationPace(longbow,
              List.of(walkThree, jumpFive, runSeven)));
    }

    @Test
    void atAWalkPaceAUnitFallenBehindMayRunToCatchUp() {
        // HammerGS: "if we set the lance to walk, we need to give permission for lagging units to run or jump"
        member(20, LEADER_HEX, 0, 5);
        BipedMek longbow = member(21, new Coords(16, 25), 1, 3);
        fake(longbow).runMP = 5;
        MovePath walkThree = moveUsing(3);
        MovePath runFive = moveUsing(5);
        MovePath sprintSeven = moveUsing(7);

        // more than a turn's walk from its place, it may run - but no further
        assertEquals(List.of(walkThree, runFive), princess.getUnitOrdersFollower().limitToFormationPace(longbow,
              List.of(walkThree, runFive, sprintSeven)));
    }

    @Test
    void aFormationUnitNeverChoosesAMoveThatBringsABuildingDownWhileAnotherIsLeft() {
        // HammerGS's playtest: narrowed by its lance's pace, an 85-ton Stalker stepped into the CF 15 building at 1119
        // rather than stand still; the building came down and left it prone
        BipedMek stalker = member(20, LEADER_HEX, 0, 3);
        member(21, new Coords(16, 25), 1, 3);
        fake(stalker).weight = 85.0;
        Coords lightBuilding = LEADER_HEX.translated(NORTH, 1);
        board.getHex(lightBuilding).addTerrain(new Terrain(Terrains.BUILDING, 1));
        board.getHex(lightBuilding).addTerrain(new Terrain(Terrains.BLDG_CF, 15));
        board.getHex(lightBuilding).addTerrain(new Terrain(Terrains.BLDG_ELEV, 1));
        Hex[] hexes = new Hex[WIDTH * HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = board.getHex(index % WIDTH, index / WIDTH);
        }
        board.newData(WIDTH, HEIGHT, hexes, null);
        MovePath intoTheBuilding = moveThrough(lightBuilding);
        MovePath roundIt = moveThrough(LEADER_HEX.translated(NORTH_EAST, 1));

        assertEquals(List.of(roundIt), princess.getUnitOrdersFollower().limitToFormationPace(stalker,
              List.of(intoTheBuilding, roundIt)));
        // with no other move left, the bot keeps it
        assertEquals(List.of(intoTheBuilding), princess.getUnitOrdersFollower().limitToFormationPace(stalker,
              List.of(intoTheBuilding)));
    }

    private static MovePath moveThrough(Coords hex) {
        MovePath path = moveTo(hex, 1);
        MoveStep step = mock(MoveStep.class);
        when(step.getPosition()).thenReturn(hex);
        when(path.getStepVector()).thenReturn(new Vector<>(List.of(step)));
        return path;
    }

    @Test
    void atARunPaceEachUnitMayRunUpToItsOwnRun() {
        BipedMek grasshopper = member(20, LEADER_HEX, 0, 5);
        grasshopper.setUnitOrders(grasshopper.getUnitOrders().withFormation(paced(0, FormationPace.RUN,
              ContactRule.BREAK)));
        BipedMek longbow = member(21, new Coords(16, 25), 1, 3);
        longbow.setUnitOrders(longbow.getUnitOrders().withFormation(paced(1, FormationPace.RUN, ContactRule.BREAK)));
        fake(grasshopper).runMP = 8;
        fake(longbow).runMP = 5;
        MovePath runFive = moveUsing(5);
        MovePath runEight = moveUsing(8);
        MovePath sprintTen = moveUsing(10);

        assertEquals(List.of(runFive, runEight), princess.getUnitOrdersFollower().limitToFormationPace(grasshopper,
              List.of(runFive, runEight, sprintTen)));
        assertEquals(List.of(runFive), princess.getUnitOrdersFollower().limitToFormationPace(longbow,
              List.of(runFive, runEight, sprintTen)));
    }

    @Test
    void aLanceThatBrokeOffToFightIsNotPaced() {
        BipedMek leader = member(20, LEADER_HEX, 0, 6);
        leader.setUnitOrders(leader.getUnitOrders().withFightState(FightState.FIGHTING));
        member(21, new Coords(16, 25), 1, 3);
        MovePath walkThree = moveUsing(3);
        MovePath runNine = moveUsing(9);

        assertEquals(List.of(walkThree, runNine), princess.getUnitOrdersFollower().limitToFormationPace(leader,
              List.of(walkThree, runNine)));
    }

    @Test
    void aFormationFormsAtTheEndOfItsRouteInsteadOfStoppingWhereItStands() {
        // HammerGS's playtest: an Echelon Left whose one waypoint was already within 3 hexes of every unit stopped
        // where it stood, roughly abreast, because every unit counted the waypoint itself as "arrived".
        // Now the leader ends on the waypoint and the follower in its slot around it.
        Coords waypoint = Coords.parseHexNumber("0906");
        FormationOrder echelonLeft = new FormationOrder(FormationShape.ECHELON_LEFT, 20, 2, 0, FormationPace.WALK,
              ContactRule.HOLD);
        BipedMek wolverine = member(20, Coords.parseHexNumber("1206"), 0, 5);
        wolverine.setUnitOrders(UnitOrders.NONE.withRoute(List.of(waypoint)).withFacings(0, 0)
              .withFormation(echelonLeft));
        BipedMek firestarter = member(21, Coords.parseHexNumber("1203"), 1, 6);
        firestarter.setUnitOrders(UnitOrders.NONE.withRoute(List.of(waypoint)).withFacings(0, 0)
              .withFormation(new FormationOrder(FormationShape.ECHELON_LEFT, 20, 2, 1, FormationPace.WALK,
                    ContactRule.HOLD)));

        // the leader has to stand on its waypoint exactly, since the shape is laid out around that hex
        assertFalse(princess.getUnitOrdersFollower().isAtRouteEnd(wolverine));
        assertEquals(0, princess.getUnitOrdersFollower().arrivalRadius(wolverine));
        assertFalse(princess.getUnitOrdersFollower().isAtRouteEnd(firestarter));
        // once the leader stands on its waypoint, facing north when stopped, Echelon Left steps back to the
        // south-west of it; before that the follower keeps its place beside the leader still on its way
        wolverine.setPosition(waypoint);
        Coords slot = waypoint.translated(SOUTH_WEST, 2);
        assertEquals(Optional.of(slot), princess.getUnitOrdersFollower().getFormationSlot(firestarter));

        firestarter.setPosition(slot);
        assertTrue(princess.getUnitOrdersFollower().isAtRouteEnd(wolverine));
        assertTrue(princess.getUnitOrdersFollower().isAtRouteEnd(firestarter));
    }

    @Test
    void aFollowerOneHexOffItsSlotHasNotArrived() {
        // HammerGS's playtest: a Column's Centurion stopped one hex beside its slot and held there, because anywhere
        // within a hex counted as arrived.
        Coords waypoint = Coords.parseHexNumber("1622");
        BipedMek grasshopper = member(20, waypoint, 0, 3);
        grasshopper.setUnitOrders(UnitOrders.NONE.withRoute(List.of(waypoint)).withFacings(0, 0).withFormation(
              new FormationOrder(FormationShape.COLUMN, 20, 2, 0, FormationPace.WALK, ContactRule.HOLD)));
        BipedMek centurion = member(21, Coords.parseHexNumber("1524"), 1, 4);
        centurion.setUnitOrders(UnitOrders.NONE.withRoute(List.of(waypoint)).withFacings(0, 0).withFormation(
              new FormationOrder(FormationShape.COLUMN, 20, 2, 1, FormationPace.WALK, ContactRule.HOLD)));
        Coords slot = Coords.parseHexNumber("1624");
        assertEquals(Optional.of(slot), princess.getUnitOrdersFollower().getFormationSlot(centurion));

        assertFalse(princess.getUnitOrdersFollower().isAtRouteEnd(centurion));
        assertEquals(0, princess.getUnitOrdersFollower().arrivalRadius(centurion));

        centurion.setPosition(slot);
        assertTrue(princess.getUnitOrdersFollower().isAtRouteEnd(centurion));
    }

    @Test
    void aSlotHeldByAUnitOutsideTheFormationMovesBesideIt() {
        member(20, NORTH_WAYPOINT, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        Coords ideal = NORTH_WAYPOINT.translated(SOUTH_EAST, 2);
        BipedMek bystander = new BipedMek();
        bystander.setId(30);
        bystander.setOwner(bot);
        game.addEntity(bystander);
        bystander.setDeployed(true);
        bystander.setPosition(ideal);

        Coords slot = princess.getUnitOrdersFollower().getFormationSlot(second).orElseThrow();

        assertEquals(1, slot.distance(ideal));
    }

    @Test
    void theLeaderDeploysFirstAndMembersDeployInTheirSlots() {
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        leader.setDeployed(false);
        second.setDeployed(false);
        second.setPosition(null);
        game.setCurrentRound(1);
        GameTurn turn = mock(GameTurn.class);
        when(turn.isValidEntity(any(Entity.class), any(Game.class))).thenReturn(true);

        // the game would deploy the member first; the leader goes first so the member can form on it
        assertEquals(20, princess.getUnitOrdersFollower().chooseUnitToDeploy(21, turn));
        Coords slot = LEADER_HEX.translated(SOUTH_EAST, 2);
        assertTrue(princess.getUnitOrdersFollower().getDeploymentSlot(second, List.of(slot)).isEmpty());

        leader.setDeployed(true);
        assertEquals(Optional.of(slot), princess.getUnitOrdersFollower().getDeploymentSlot(second, List.of(slot)));

        List<Coords> legalHexes = List.of(new Coords(2, 2), slot.translated(SOUTH, 1), new Coords(28, 28));
        assertEquals(slot.translated(SOUTH, 1),
              princess.getUnitOrdersFollower().preferDeploymentSlot(second, legalHexes).get(0));
    }

    @Test
    void aLanceGivenAFacingInTheLobbyDeploysItsShapeAlongThatFacing() {
        // HammerGS: set a facing in the lobby. A lance ordered to face south-east when stopped lays its Echelon Right
        // out along south-east, although its route heads north.
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
        leader.setUnitOrders(leader.getUnitOrders().withFacings(UnitOrders.FACING_AUTO, SOUTH_EAST));
        leader.setDeployed(true);
        BipedMek second = member(21, null, 1, 4);
        second.setDeployed(false);
        Coords slot = LEADER_HEX.translated(SOUTH_WEST, 2);

        assertEquals(Optional.of(slot), princess.getUnitOrdersFollower().getDeploymentSlot(second, List.of(slot)));
    }

    @Test
    void theBotFacesTheEnemyDeploymentZone() {
        // HammerGS: deploy facing the enemy's deployment zone. An enemy deploying along the north edge, three rows
        // deep, puts the zone's middle at the top of the board, halfway across.
        doReturn(bot).when(princess).getLocalPlayer();
        bot.setTeam(1);
        Player enemy = new Player(2, "Clan Wolf");
        enemy.setTeam(2);
        enemy.setStartingPos(Board.START_N);
        game.addPlayer(2, enemy);
        BipedMek enemyMek = new BipedMek();
        enemyMek.setId(40);
        enemyMek.setOwner(enemy);
        game.addEntity(enemyMek);

        Coords center = princess.getEnemyDeploymentCenter(board).orElseThrow();

        assertTrue(center.getY() <= 2, "zone middle " + center.getBoardNum() + " is not along the north edge");
        assertEquals(WIDTH / 2, center.getX(), 1);
    }

    @Test
    void aZoneTooShallowForAVeeDeploysTheLanceInALineWithEveryUnitInTheZone() {
        // HammerGS's playtest: a Vee lance deploying in a two-row zone at the board's edge scattered, because the
        // Vee's arms reach four rows ahead of the leader and every slot fell outside the zone.
        List<Coords> zone = new ArrayList<>();
        for (int x = 0; x < WIDTH; x++) {
            zone.add(new Coords(x, HEIGHT - 2));
            zone.add(new Coords(x, HEIGHT - 1));
        }
        List<BipedMek> lance = new ArrayList<>();
        for (int slot = 0; slot < 4; slot++) {
            BipedMek mek = member(20 + slot, null, slot, 4);
            mek.setDeployed(false);
            mek.setUnitOrders(UnitOrders.NONE.withFormation(
                  new FormationOrder(FormationShape.VEE, 20, 2, slot, FormationPace.WALK, ContactRule.BREAK)));
            lance.add(mek);
        }

        List<Coords> leaderHexes = princess.getUnitOrdersFollower().preferFormationFit(lance.get(0), zone);

        assertFalse(leaderHexes.isEmpty());
        assertTrue(leaderHexes.size() < zone.size());
        BipedMek leader = lance.get(0);
        leader.setPosition(leaderHexes.get(0));
        leader.setDeployed(true);
        List<Coords> slots = new ArrayList<>();
        for (BipedMek mek : lance.subList(1, lance.size())) {
            Coords slot = princess.getUnitOrdersFollower().getDeploymentSlot(mek, zone).orElseThrow();
            assertTrue(zone.contains(slot), "slot " + slot.getBoardNum() + " is outside the zone");
            assertFalse(slots.contains(slot));
            slots.add(slot);
        }
    }

    private static FormationOrder keptTogether(int slot) {
        return new FormationOrder(FormationShape.WEDGE, 20, 2, slot, FormationPace.WALK, ContactRule.BREAK, true);
    }

    /** A leader on the first of two waypoints and one other unit far from its slot, both keeping together. */
    /**
     * A lance kept together whose route turns from a Wedge into a Column at its first waypoint, so it stops there to
     * re-form; the leader stands on that waypoint and the second unit is well short of its slot.
     */
    private List<BipedMek> lanceKeepingTogether() {
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        WaypointFormation column = new WaypointFormation(FormationShape.COLUMN, 2, FormationPace.WALK,
              ContactRule.BREAK, true);
        List<WaypointOrder> wedgeThenColumn = List.of(WaypointOrder.PASS_THROUGH,
              new WaypointOrder(UnitOrders.FACING_AUTO, 0, column));
        BipedMek leader = member(20, NORTH_WAYPOINT, 0, 5);
        leader.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, eastWaypoint), wedgeThenColumn)
              .withFormation(keptTogether(0)));
        BipedMek second = member(21, new Coords(16, 25), 1, 3);
        second.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, eastWaypoint), wedgeThenColumn)
              .withFormation(keptTogether(1)));
        doReturn(List.<Entity>of(leader, second)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
        return List.of(leader, second);
    }

    @Test
    void aLeaderKeepingTogetherWaitsForAUnitWithFurtherToGo() {
        // HammerGS's playtest: capped at the Longbow's 3 MP, the Grasshopper still reached its waypoint six turns
        // before the Longbow reached its slot, because the Longbow's slot lay further off. The leader now holds back.
        BipedMek grasshopper = member(20, new Coords(14, 14), 0, 5);
        grasshopper.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)).withFormation(keptTogether(0)));
        BipedMek longbow = member(21, new Coords(14, 28), 1, 3);
        longbow.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)).withFormation(keptTogether(1)));
        MovePath standStill = moveTo(new Coords(14, 14), 0);
        MovePath walkThreeOn = moveTo(new Coords(14, 11), 3);

        // twelve hexes to go is four turns at 3 MP; the Longbow needs eight or more, so the Grasshopper waits
        assertEquals(List.of(standStill), princess.getUnitOrdersFollower().limitToFormationPace(grasshopper,
              List.of(standStill, walkThreeOn)));

        longbow.setPosition(princess.getUnitOrdersFollower().getFormationSlot(longbow).orElseThrow());
        assertEquals(List.of(standStill, walkThreeOn), princess.getUnitOrdersFollower()
              .limitToFormationPace(grasshopper, List.of(standStill, walkThreeOn)));
    }

    @Test
    void aLanceChangingShapeAtAWaypointKeepsItsShapeThereAndReformsBeforeGoingOn() {
        // HammerGS: stay in the formation until the waypoint, form the Column there, then go on in Column
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        WaypointFormation wedge = new WaypointFormation(FormationShape.WEDGE, 2, FormationPace.WALK,
              ContactRule.BREAK, false);
        WaypointFormation column = new WaypointFormation(FormationShape.COLUMN, 2, FormationPace.WALK,
              ContactRule.BREAK, false);
        WaypointOrder formColumnThere = new WaypointOrder(UnitOrders.FACING_AUTO, WaypointOrder.HoldMode.PASS, 0,
              wedge, false, column);
        UnitOrders route = UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, eastWaypoint),
              List.of(formColumnThere, new WaypointOrder(UnitOrders.FACING_AUTO, 0, column)));
        BipedMek leader = member(20, LEADER_HEX, 0, 5);
        leader.setUnitOrders(route.withFormation(paced(0, FormationPace.WALK, ContactRule.BREAK)));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        second.setUnitOrders(route.withFormation(paced(1, FormationPace.WALK, ContactRule.BREAK)));
        doReturn(List.<Entity>of(leader, second)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        // on the way the lance keeps the Wedge
        assertEquals(FormationShape.WEDGE, follower.activeFormation(second).orElseThrow().getShape());

        // at the waypoint it re-forms as a Column, and the leader waits there for it, though not kept together
        leader.setPosition(NORTH_WAYPOINT);
        assertEquals(FormationShape.COLUMN, follower.activeFormation(second).orElseThrow().getShape());
        follower.advanceRoutes();
        assertEquals(2, leader.getUnitOrders().getRoute().size());
    }

    @Test
    void aNewRouteIsCalledOnceByItsFirstNavPoint() {
        // a fellow dev's example: "Charlie Lance, proceed to Nav Point Gamma"
        Coords secondWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 4);
        UnitOrders route = UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, secondWaypoint),
              List.of(WaypointOrder.PASS_THROUGH.withNavNumber(1), WaypointOrder.PASS_THROUGH.withNavNumber(2)));
        BipedMek scout = loneUnit(32, LEADER_HEX, route);
        doReturn(List.<Entity>of(scout)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        follower.advanceRoutes();
        verify(princess).sendChat(argThat((String call) -> call.contains("proceeding to Nav Point Alpha ("
              + NORTH_WAYPOINT.getBoardNum() + ")")), any(Level.class));

        // ticking a waypoint off the front is the same order, not a new one
        scout.setUnitOrders(scout.getUnitOrders().withNextWaypointReached());
        game.setCurrentRound(2);
        follower.advanceRoutes();
        verify(princess, times(1)).sendChat(argThat((String call) -> call.contains("proceeding to")),
              any(Level.class));
    }

    @Test
    void aFormationsLeaderStandsOnEveryFlag() {
        // HammerGS's playtest: the Grasshopper cut the corner at 1526 by two hexes, off the route it was leading
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        BipedMek leader = member(20, NORTH_WAYPOINT.translated(SOUTH, 2), 0, 5);
        leader.setUnitOrders(leader.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT, eastWaypoint)));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        second.setUnitOrders(second.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT, eastWaypoint)));
        doReturn(List.<Entity>of(leader, second)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        assertEquals(0, follower.arrivalRadius(leader));
        follower.advanceRoutes();
        assertEquals(2, leader.getUnitOrders().getRoute().size());

        leader.setPosition(NORTH_WAYPOINT);
        follower.advanceRoutes();
        assertEquals(List.of(eastWaypoint), leader.getUnitOrders().getRoute());
    }

    @Test
    void aLeaderHeldBackForTheLastUnitStaysOnItsHex() {
        // HammerGS's playtest: held back for the Longbow, the Grasshopper wandered sideways from 1906 to 1604 and back
        BipedMek grasshopper = member(20, new Coords(14, 14), 0, 5);
        grasshopper.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)).withFormation(keptTogether(0)));
        BipedMek longbow = member(21, new Coords(14, 28), 1, 3);
        longbow.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)).withFormation(keptTogether(1)));
        MovePath standStill = moveTo(new Coords(14, 14), 0);
        MovePath sideways = moveTo(new Coords(12, 14), 2);

        assertEquals(List.of(standStill), princess.getUnitOrdersFollower().limitToFormationPace(grasshopper,
              List.of(standStill, sideways)));
    }

    @Test
    void aUnitOnItsWayFacesTheRouteWithItsLegsAndOnlyAUnitStoppingLeavesItToATwist() {
        // HammerGS's playtest: units ended moves a side off the route because a torso twist would cover it
        BipedMek scout = loneUnit(33, LEADER_HEX, UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        assertEquals(0, follower.twistAllowance(scout, LEADER_HEX.translated(NORTH, 3)));
        assertEquals(UnitOrdersFollower.twistReach(scout), follower.twistAllowance(scout, NORTH_WAYPOINT));
    }

    @Test
    void aColumnStoppedOnItsLastFlagFormsBehindTheFacingSetThere() {
        // HammerGS's playtest: told to face north at 2403, the Column trailed off south-west along the way it came
        Coords approach = NORTH_WAYPOINT.translated(SOUTH_WEST, 4);
        BipedMek leader = member(20, approach, 0, 5);
        leader.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT), List.of(new WaypointOrder(NORTH, 0)))
              .withFormation(columnOf(0)));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        second.setUnitOrders(leader.getUnitOrders().withFormation(columnOf(1)));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        follower.getFormationSlot(second);

        leader.setPosition(NORTH_WAYPOINT);

        assertEquals(Optional.of(NORTH_WAYPOINT.translated(SOUTH, 2)), follower.getFormationSlot(second));
    }

    private static MovePath moveTo(Coords end, int movementPoints) {
        MovePath path = moveUsing(movementPoints);
        when(path.getFinalCoords()).thenReturn(end);
        return path;
    }

    @Test
    void aFormationKeptTogetherAdvancesAtItsSlowestUnitsSpeed() {
        // HammerGS's playtest: without it a Grasshopper outran a Longbow walking 3 and the lance spread across the map
        List<BipedMek> lance = lanceKeepingTogether();
        MovePath walkThree = moveUsing(3);
        MovePath walkFive = moveUsing(5);

        assertEquals(List.of(walkThree), princess.getUnitOrdersFollower().limitToFormationPace(lance.get(0),
              List.of(walkThree, walkFive)));
    }

    @Test
    void aLanceKeptTogetherPassesAWaypointWhereItsShapeStaysTheSame() {
        // HammerGS's playtest: stopping to re-form at every waypoint a few hexes apart cost 12 of 16 rounds
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        BipedMek leader = member(20, NORTH_WAYPOINT, 0, 5);
        leader.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, eastWaypoint))
              .withFormation(keptTogether(0)));
        BipedMek second = member(21, new Coords(16, 25), 1, 3);
        second.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, eastWaypoint))
              .withFormation(keptTogether(1)));
        doReturn(List.<Entity>of(leader, second)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
        second.setPosition(princess.getUnitOrdersFollower().getFormationSlot(second).orElseThrow());

        princess.getUnitOrdersFollower().advanceRoutes();

        assertEquals(List.of(eastWaypoint), leader.getUnitOrders().getRoute());
    }

    @Test
    void aLanceThatHasComeApartStopsToFormUpEvenWhereItsShapeStaysTheSame() {
        // HammerGS's playtest: a lance that never formed up passed its first flag and went on strung out over half
        // the map (2026-10-01); a lance that is together still passes, as above
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        BipedMek leader = member(20, NORTH_WAYPOINT, 0, 5);
        leader.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, eastWaypoint))
              .withFormation(keptTogether(0)));
        BipedMek second = member(21, new Coords(16, 25), 1, 3);
        second.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, eastWaypoint))
              .withFormation(keptTogether(1)));
        doReturn(List.<Entity>of(leader, second)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));

        princess.getUnitOrdersFollower().advanceRoutes();

        assertEquals(List.of(NORTH_WAYPOINT, eastWaypoint), leader.getUnitOrders().getRoute());
        assertTrue(princess.getUnitOrdersFollower().isWaitingForFormation(leader));
    }

    @Test
    void aWaypointsFacingTurnsTheShapeOnlyWhereTheLanceStops() {
        // HammerGS's playtest: a facing of NE on a waypoint the Column only passed laid its tail back to the
        // south-west, and the last Mek walked seven hexes off the route and back
        Coords northAgain = NORTH_WAYPOINT.translated(NORTH, 1);
        WaypointOrder faceNorthEast = new WaypointOrder(1, 0);
        BipedMek leader = member(20, NORTH_WAYPOINT, 0, 5);
        leader.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, northAgain),
              List.of(faceNorthEast, WaypointOrder.PASS_THROUGH)).withFormation(echelonRightOf(0)));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        second.setUnitOrders(leader.getUnitOrders().withFormation(echelonRightOf(1)));

        // passing through, the Echelon Right lies along the way north, stepping back south-east
        assertEquals(Optional.of(NORTH_WAYPOINT.translated(SOUTH_EAST, 2)),
              princess.getUnitOrdersFollower().getFormationSlot(second));

        // holding there, it faces the way it was told, north-east, stepping back to the south
        WaypointOrder holdFacingNorthEast = new WaypointOrder(1, 2);
        leader.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT, northAgain),
              List.of(holdFacingNorthEast, WaypointOrder.PASS_THROUGH)).withFormation(echelonRightOf(0)));
        assertEquals(Optional.of(NORTH_WAYPOINT.translated(SOUTH, 2)),
              princess.getUnitOrdersFollower().getFormationSlot(second));
    }

    private static FormationOrder echelonRightOf(int slot) {
        return new FormationOrder(FormationShape.ECHELON_RIGHT, 20, 2, slot, FormationPace.WALK, ContactRule.HOLD);
    }

    private static FormationOrder columnOf(int slot) {
        return new FormationOrder(FormationShape.COLUMN, 20, 2, slot, FormationPace.WALK, ContactRule.HOLD);
    }

    @Test
    void aLanceWaitingAtAFlagFacesOnToTheNextOne() {
        // HammerGS's playtest: waiting to re-form at a flag, the lance had turned its back on the next one
        List<BipedMek> lance = lanceKeepingTogether();
        BipedMek leader = lance.get(0);
        BipedMek second = lance.get(1);
        Coords eastWaypoint = leader.getUnitOrders().getRoute().get(1);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        game.setCurrentRound(3);
        follower.advanceRoutes();
        assertTrue(follower.isWaitingForFormation(leader));

        assertEquals(NORTH_WAYPOINT.direction(eastWaypoint), follower.stoppedFacing(leader));
        Coords slot = follower.getFormationSlot(second).orElseThrow();
        assertEquals(slot.direction(eastWaypoint), follower.orderedFacing(second, slot));
    }

    @Test
    void aLeaderKeepingTogetherWaitsAtAWaypointUntilItsFormationFormsUp() {
        List<BipedMek> lance = lanceKeepingTogether();
        BipedMek leader = lance.get(0);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        game.setCurrentRound(3);

        follower.advanceRoutes();
        assertEquals(NORTH_WAYPOINT, leader.getUnitOrders().getNextWaypoint().orElseThrow());
        assertTrue(follower.isHolding(leader));

        lance.get(1).setPosition(follower.getFormationSlot(lance.get(1)).orElseThrow());
        game.setCurrentRound(4);
        follower.advanceRoutes();
        assertEquals(1, leader.getUnitOrders().getRoute().size());
        assertFalse(follower.isWaitingForFormation(leader));
    }

    @Test
    void aLeaderWaitingBesideAnOccupiedFlagStepsOntoItOnceItIsClear() {
        // HammerGS's playtest: the Stalker began waiting a hex short of 1223 while another unit stood on it, and the
        // wait then held it there for good - its lance formed a Line round the flag and never moved on
        List<BipedMek> lance = lanceKeepingTogether();
        BipedMek leader = lance.get(0);
        leader.setPosition(NORTH_WAYPOINT.translated(SOUTH, 1));
        BipedMek bystander = new BipedMek();
        bystander.setId(30);
        bystander.setOwner(bot);
        game.addEntity(bystander);
        bystander.setDeployed(true);
        bystander.setPosition(NORTH_WAYPOINT);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        game.setCurrentRound(3);
        follower.advanceRoutes();
        assertTrue(follower.isWaitingForFormation(leader));

        bystander.setPosition(NORTH_WAYPOINT.translated(NORTH, 3));
        game.setCurrentRound(4);
        follower.advanceRoutes();

        assertFalse(follower.isWaitingForFormation(leader));
        assertFalse(follower.isHolding(leader));
    }

    @Test
    void aLeaderStopsWaitingForAUnitStillComingAfterSixRounds() {
        List<BipedMek> lance = lanceKeepingTogether();
        BipedMek leader = lance.get(0);
        BipedMek second = lance.get(1);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        for (int round = 3; round < 3 + UnitOrdersFollower.MAXIMUM_REFORM_WAIT_ROUNDS; round++) {
            game.setCurrentRound(round);
            follower.advanceRoutes();
            assertEquals(2, leader.getUnitOrders().getRoute().size(), "moved on early in round " + round);
            // a hex nearer each round: slow, but getting there, so the lance keeps waiting
            second.setPosition(second.getPosition().translated(NORTH, 1));
        }

        game.setCurrentRound(3 + UnitOrdersFollower.MAXIMUM_REFORM_WAIT_ROUNDS);
        follower.advanceRoutes();
        assertEquals(1, leader.getUnitOrders().getRoute().size());
    }

    @Test
    void aLeaderStopsWaitingForAUnitThatGetsNoCloserAfterThreeRounds() {
        // HammerGS: a unit that makes no headway for three rounds - wading, blocked, going back and forth - is not
        // waited for; it follows on and rejoins the shape when it catches up (2026-09-27)
        List<BipedMek> lance = lanceKeepingTogether();
        BipedMek leader = lance.get(0);
        BipedMek second = lance.get(1);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        for (int round = 3; round < 3 + UnitOrdersFollower.ROUNDS_WITHOUT_PROGRESS; round++) {
            game.setCurrentRound(round);
            follower.advanceRoutes();
            assertEquals(2, leader.getUnitOrders().getRoute().size(), "moved on early in round " + round);
        }

        game.setCurrentRound(3 + UnitOrdersFollower.ROUNDS_WITHOUT_PROGRESS);
        follower.advanceRoutes();

        assertTrue(follower.isFallingBehind(second));
        assertEquals(1, leader.getUnitOrders().getRoute().size());
    }

    @Test
    void aUnitThatCannotMoveDropsOutAndTheOthersCloseUp() {
        // HammerGS: an immobile unit holds and fights where it is while the lance closes up and moves on (2026-09-27)
        member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        BipedMek third = member(22, new Coords(12, 25), 2, 4);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        Optional<Coords> secondPlace = follower.getFormationSlot(second);

        fake(second).immobile = true;

        assertTrue(follower.isOutOfAction(second));
        assertEquals(Optional.empty(), follower.getFormationSlot(second));
        assertEquals(secondPlace, follower.getFormationSlot(third));
    }

    @Test
    void aUnitDownTwoRoundsIsLeftBehind() {
        // HammerGS: the lance waits for a fallen unit to get up, but one still down after two rounds counts as out
        // of action (2026-09-27)
        List<BipedMek> lance = lanceKeepingTogether();
        BipedMek second = lance.get(1);
        fake(second).prone = true;
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        for (int round = 3; round < 3 + UnitOrdersFollower.PRONE_ROUNDS_BEFORE_DROPPED; round++) {
            game.setCurrentRound(round);
            follower.advanceRoutes();
            assertFalse(follower.isOutOfAction(second), "dropped early in round " + round);
        }
        game.setCurrentRound(3 + UnitOrdersFollower.PRONE_ROUNDS_BEFORE_DROPPED);
        follower.advanceRoutes();
        assertTrue(follower.isOutOfAction(second));

        fake(second).prone = false;
        game.setCurrentRound(4 + UnitOrdersFollower.PRONE_ROUNDS_BEFORE_DROPPED);
        follower.advanceRoutes();
        assertFalse(follower.isOutOfAction(second), "back on its feet, it rejoins");
    }

    @Test
    void theSecondInCommandTakesCommandWhenTheCommanderIsLost() {
        // HammerGS: as in a tank platoon, the second-in-command (place 3) takes over, not the commander's wingman
        BipedMek commander = member(20, LEADER_HEX, 0, 3);
        BipedMek wingman = member(21, new Coords(16, 25), 1, 4);
        BipedMek secondInCommand = member(22, new Coords(12, 25), 2, 4);
        member(23, new Coords(10, 25), 3, 4);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        fake(commander).immobile = true;

        assertEquals(Optional.empty(), follower.getFormationSlot(secondInCommand), "the new commander has no slot");
        assertTrue(follower.getFormationSlot(wingman).isPresent());
    }

    @Test
    void aUnitOnARouteFacesTheNextFlagButNeverTurnsItsBackToTheEnemy() {
        // HammerGS: the player's route is the plan, so a waypoint with no facing set faces toward the next flag, and
        // an enemy off to a side does not turn the unit; but never the rear arc into the line of fire (2026-09-27)
        BipedMek scout = loneUnit(30, LEADER_HEX, UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        assertEquals(NORTH, follower.orderedFacing(scout, LEADER_HEX));

        Entity enemy = mock(Entity.class);
        Coords offToTheSide = LEADER_HEX.translated(SOUTH_WEST, 4);
        when(enemy.getPosition()).thenReturn(offToTheSide);
        enemies.add(enemy);
        assertEquals(NORTH, follower.orderedFacing(scout, LEADER_HEX));
        assertEquals(NORTH, follower.facingThatStandsFor(scout, NORTH, LEADER_HEX, offToTheSide));

        Coords behind = LEADER_HEX.translated(SOUTH, 4);
        assertEquals(UnitOrders.FACING_AUTO, follower.facingThatStandsFor(scout, NORTH, LEADER_HEX, behind));
    }

    @Test
    void aUnitInFormationFacesTheWayTheFormationFacesNotBackAtTheFlag() {
        // HammerGS's town playtest: the Centurion, its slot beyond the flag, faced south to look back at it
        member(20, LEADER_HEX, 0, 5);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        Coords beyondTheFlag = NORTH_WAYPOINT.translated(NORTH, 1);

        assertEquals(NORTH, princess.getUnitOrdersFollower().orderedFacing(second, beyondTheFlag));
    }

    @Test
    void aSlotWalledOffFromTheUnitFoldsItIntoTheColumn() {
        // HammerGS's town playtest: a Longbow waded toward a Line slot behind a building row while its lance waited
        member(20, NORTH_WAYPOINT, 0, 5);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        Coords echelonSlot = NORTH_WAYPOINT.translated(SOUTH_EAST, 2);
        for (int direction = 0; direction < 6; direction++) {
            Coords wall = echelonSlot.translated(direction);
            board.getHex(wall).setLevel(CLIFF_LEVEL);
            board.getHex(wall).addTerrain(new Terrain(Terrains.IMPASSABLE, 1));
        }

        // the Echelon Right slot can be stood in but not reached; the unit takes its Column place instead
        assertEquals(Optional.of(NORTH_WAYPOINT.translated(SOUTH, 2)),
              princess.getUnitOrdersFollower().getFormationSlot(second));
    }

    @Test
    void aWaypointsFormationSetsTheShapeForTheLegEndingThere() {
        // HammerGS: change the formation at each waypoint; a row reads "travel to this hex in this formation"
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
        WaypointFormation column = new WaypointFormation(FormationShape.COLUMN, 3, FormationPace.WALK,
              ContactRule.HOLD, false);
        leader.setUnitOrders(leader.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT),
              List.of(new WaypointOrder(UnitOrders.FACING_AUTO, 0, column))));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);

        // the units' own Echelon Right gives way to the leg's Column: three hexes behind the leader, facing its flag
        assertEquals(Optional.of(LEADER_HEX.translated(SOUTH, 3)),
              princess.getUnitOrdersFollower().getFormationSlot(second));
    }

    @Test
    void aColumnFollowsTheHexesItsCommanderWalked() {
        // HammerGS: in a Column the commander is at the head and the rest fall in behind, not lined up at the flag
        BipedMek leader = member(20, LEADER_HEX, 0, 5);
        leader.setUnitOrders(leader.getUnitOrders().withFormation(columnOf(0)));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        second.setUnitOrders(second.getUnitOrders().withFormation(new FormationOrder(FormationShape.COLUMN, 20, 1, 1,
              FormationPace.WALK, ContactRule.HOLD)));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        follower.getFormationSlot(second);

        // the commander walks three hexes north-east, then two north
        leader.setPosition(LEADER_HEX.translated(NORTH_EAST, 3));
        follower.getFormationSlot(second);
        leader.setPosition(LEADER_HEX.translated(NORTH_EAST, 3).translated(NORTH, 2));

        // one hex apart, the second unit's place is the hex the commander walked through one step back
        assertEquals(Optional.of(LEADER_HEX.translated(NORTH_EAST, 3).translated(NORTH, 1)),
              follower.getFormationSlot(second));
    }

    @Test
    void aLegSetToNoFormationIsTravelledByEachUnitOnItsOwn() {
        BipedMek leader = member(20, LEADER_HEX, 0, 6);
        leader.setUnitOrders(leader.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT),
              List.of(new WaypointOrder(UnitOrders.FACING_AUTO, 0, WaypointFormation.NONE))));
        BipedMek second = member(21, new Coords(16, 25), 1, 3);
        MovePath runNine = moveUsing(9);

        assertTrue(princess.getUnitOrdersFollower().getFormationSlot(second).isEmpty());
        assertEquals(List.of(runNine), princess.getUnitOrdersFollower().limitToFormationPace(leader,
              List.of(runNine)));
    }

    @Test
    void theLeaderWorksOutFromPathsAndSpeedHowLongTheLastUnitNeeds() {
        // HammerGS: the leader can see how far each unit is from its slot, over what ground, and at what speed
        BipedMek leader = member(20, NORTH_WAYPOINT, 0, 5);
        BipedMek second = member(21, LEADER_HEX, 1, 3);
        Coords slot = princess.getUnitOrdersFollower().getFormationSlot(second).orElseThrow();
        second.setPosition(slot.translated(SOUTH, 6));

        // six hexes of open ground at a walk of three
        assertEquals(2, princess.getUnitOrdersFollower().estimatedAssemblyTurns(leader));
    }

    @Test
    void aLeaderWaitingUntilInPositionMovesOnWhenTheLastUnitArrives() {
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        WaypointOrder untilInPosition = new WaypointOrder(UnitOrders.FACING_AUTO, WaypointOrder.HoldMode.ASSEMBLE, 8,
              null, false);
        BipedMek leader = member(20, NORTH_WAYPOINT, 0, 5);
        leader.setUnitOrders(leader.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT, eastWaypoint),
              List.of(untilInPosition)));
        BipedMek second = member(21, new Coords(16, 25), 1, 3);
        doReturn(List.<Entity>of(leader, second)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        game.setCurrentRound(3);
        follower.advanceRoutes();
        game.setCurrentRound(4);
        follower.advanceRoutes();
        assertTrue(follower.isHolding(leader));
        assertEquals(2, leader.getUnitOrders().getRoute().size());

        second.setPosition(follower.getFormationSlot(second).orElseThrow());
        game.setCurrentRound(5);
        follower.advanceRoutes();
        assertEquals(List.of(eastWaypoint), leader.getUnitOrders().getRoute());
    }

    @Test
    void aRouteEndingInAnExitLeavesTheBoardByTheNearestEdge() {
        // HammerGS: exit as the end of the route, instead of a separate order
        WaypointOrder exit = new WaypointOrder(UnitOrders.FACING_AUTO, WaypointOrder.HoldMode.PASS, 0, null, true);
        BipedMek scout = loneUnit(32, NORTH_WAYPOINT, UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT),
              List.of(exit)));
        doReturn(List.<Entity>of(scout)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));

        princess.getUnitOrdersFollower().advanceRoutes();

        assertEquals(EdgeOrder.EXIT_BY, scout.getUnitOrders().getEdgeOrder());
        assertEquals(OffBoardDirection.NORTH, scout.getUnitOrders().getEdge());
    }

    @Test
    void aMekTwistsToAnOrderedFacingWithinReachRatherThanTurning() {
        // HammerGS: turning in place spends movement and counts as moving; a torso twist is free
        BipedMek warhammer = loneUnit(31, NORTH_WAYPOINT, UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT))
              .withFacings(UnitOrders.FACING_AUTO, NORTH_EAST));
        warhammer.setFacing(NORTH);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();

        assertEquals(1, UnitOrdersFollower.twistReach(warhammer));
        assertEquals(NORTH_EAST, follower.orderedTwist(warhammer));

        // two hexsides off is beyond a torso twist: the legs have to turn
        warhammer.setUnitOrders(warhammer.getUnitOrders().withFacings(UnitOrders.FACING_AUTO, SOUTH_EAST));
        assertEquals(UnitOrders.FACING_AUTO, follower.orderedTwist(warhammer));
        assertEquals(2, UnitOrdersFollower.sidesApart(NORTH, SOUTH_EAST));
    }

    private BipedMek loneUnit(int unitId, Coords position, UnitOrders orders) {
        BipedMek mek = new BipedMek();
        mek.setId(unitId);
        mek.setOwner(bot);
        game.addEntity(mek);
        mek.setPosition(position);
        mek.setUnitOrders(orders);
        return mek;
    }

    @Test
    void aUnitHoldsAtAWaypointForTheTurnsSetThenMovesOn() {
        // HammerGS: a waypoint can hold the unit a number of turns, facing a set way, before it moves on
        Coords holdHex = Coords.parseHexNumber("1706");
        Coords lastHex = Coords.parseHexNumber("2204");
        BipedMek wolverine = loneUnit(30, holdHex.translated(SOUTH, 1), UnitOrders.NONE.withRoute(
              List.of(holdHex, lastHex), List.of(new WaypointOrder(NORTH_EAST, 2))));
        doReturn(List.<Entity>of(wolverine)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString());
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        game.setCurrentRound(3);

        // one hex short does not start the hold: a waypoint set to hold must be reached exactly
        assertEquals(0, follower.arrivalRadius(wolverine));
        follower.advanceRoutes();
        assertEquals(UnitOrders.NO_ROUND, wolverine.getUnitOrders().getHoldSinceRound());

        wolverine.setPosition(holdHex);
        follower.advanceRoutes();
        assertEquals(3, wolverine.getUnitOrders().getHoldSinceRound());

        game.setCurrentRound(4);
        assertTrue(follower.isHolding(wolverine));
        assertEquals(NORTH_EAST, follower.stoppedFacing(wolverine));
        follower.advanceRoutes();
        assertEquals(List.of(holdHex, lastHex), wolverine.getUnitOrders().getRoute());

        game.setCurrentRound(5);
        follower.advanceRoutes();
        assertEquals(List.of(lastHex), wolverine.getUnitOrders().getRoute());
    }

    @Test
    void aFormationLaysItsShapeOutAlongTheFacingSetOnAWaypoint() {
        BipedMek leader = member(20, NORTH_WAYPOINT, 0, 3);
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        leader.setUnitOrders(leader.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT, eastWaypoint),
              List.of(new WaypointOrder(SOUTH_WEST, 1))));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);

        // facing southwest, an Echelon Right steps back two facings round, to the north
        assertEquals(Optional.of(NORTH_WAYPOINT.translated(NORTH, 2)),
              princess.getUnitOrdersFollower().getFormationSlot(second));
    }

    @Test
    void aFormationUnitOnItsSlotHoldsWhileItsLeaderHolds() {
        BipedMek leader = member(20, NORTH_WAYPOINT, 0, 3);
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        leader.setUnitOrders(leader.getUnitOrders().withRoute(List.of(NORTH_WAYPOINT, eastWaypoint),
              List.of(new WaypointOrder(SOUTH_WEST, 2))).withHoldStarted(3));
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        game.setCurrentRound(4);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        Coords slot = follower.getFormationSlot(second).orElseThrow();

        assertFalse(follower.isHolding(second));

        second.setPosition(slot);
        assertTrue(follower.isHolding(second));
        assertEquals(SOUTH_WEST, follower.stoppedFacing(second));
    }
}
