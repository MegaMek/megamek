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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.moves.MovePath;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.UnitOrders;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
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
    private static final int SOUTH_EAST = 2;
    private static final int SOUTH = 3;
    private static final int SOUTH_WEST = 4;
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

    private BipedMek member(int unitId, Coords position, int slot, int walkMP) {
        BipedMek mek = spy(new BipedMek());
        mek.setId(unitId);
        mek.setOwner(bot);
        game.addEntity(mek);
        mek.setPosition(position);
        doReturn(walkMP).when(mek).getWalkMP();
        mek.setUnitOrders(UnitOrders.NONE.withRoute(List.of(NORTH_WAYPOINT)).withFormation(
              new FormationOrder(FormationShape.ECHELON_RIGHT, 20, 2, slot, FormationPace.WALK, ContactRule.BREAK)));
        return mek;
    }

    @Test
    void aFollowersTargetIsItsSlotAroundTheLeadersWaypoint() {
        member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);

        // heading north to the waypoint, the Echelon Right forms there, stepping back south-east
        assertEquals(Optional.of(NORTH_WAYPOINT.translated(SOUTH_EAST, 2)),
              princess.getUnitOrdersFollower().getFormationSlot(second));
    }

    @Test
    void theSlotStaysPutWhileTheLeaderMoves() {
        // HammerGS's playtest: slots worked out around the leader's moving hex left the Wedge bunched up and drifting.
        // The slot is fixed by where the leader is going, so each unit has one hex to make for all the way.
        game.setPhase(GamePhase.MOVEMENT);
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        Optional<Coords> slot = princess.getUnitOrdersFollower().getFormationSlot(second);

        leader.setPosition(LEADER_HEX.translated(NORTH, 3));
        leader.setDone(true);

        assertEquals(slot, princess.getUnitOrdersFollower().getFormationSlot(second));
    }

    @Test
    void atAWaypointPartWayTheShapeFacesTheNextLeg() {
        Coords eastWaypoint = NORTH_WAYPOINT.translated(SOUTH_EAST, 8);
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
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
        member(20, LEADER_HEX, 0, 3);
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
    void theNextUnitLeadsWhenTheLeaderIsGone() {
        BipedMek leader = member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        member(22, new Coords(18, 26), 2, 5);
        leader.setDestroyed(true);

        assertTrue(princess.getUnitOrdersFollower().getFormationSlot(second).isEmpty());
    }

    @Test
    void aFormationBreaksOnContactButOneOrderedToHoldDoesNot() {
        member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);
        Entity enemy = mock(Entity.class);
        when(enemy.getPosition()).thenReturn(LEADER_HEX.translated(0, 5));
        when(enemy.getBoardId()).thenReturn(0);
        enemies.add(enemy);

        assertTrue(princess.getUnitOrdersFollower().getFormationSlot(second).isEmpty());

        second.setUnitOrders(second.getUnitOrders().withFormation(
              new FormationOrder(FormationShape.ECHELON_RIGHT, 20, 2, 1, FormationPace.WALK, ContactRule.HOLD)));
        assertTrue(princess.getUnitOrdersFollower().getFormationSlot(second).isPresent());
    }

    private static MovePath moveUsing(int movementPoints) {
        MovePath path = mock(MovePath.class);
        when(path.getMpUsed()).thenReturn(movementPoints);
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

        assertEquals(List.of(walkThree, jumpFive), princess.getUnitOrdersFollower().limitToFormationPace(grasshopper,
              List.of(walkThree, jumpFive, runSeven)));
        assertEquals(List.of(walkThree), princess.getUnitOrdersFollower().limitToFormationPace(longbow,
              List.of(walkThree, jumpFive, runSeven)));
    }

    @Test
    void atARunPaceEachUnitMayRunUpToItsOwnRun() {
        BipedMek grasshopper = member(20, LEADER_HEX, 0, 5);
        grasshopper.setUnitOrders(grasshopper.getUnitOrders().withFormation(paced(0, FormationPace.RUN,
              ContactRule.BREAK)));
        BipedMek longbow = member(21, new Coords(16, 25), 1, 3);
        longbow.setUnitOrders(longbow.getUnitOrders().withFormation(paced(1, FormationPace.RUN, ContactRule.BREAK)));
        doReturn(8).when(grasshopper).getRunMP();
        doReturn(5).when(longbow).getRunMP();
        MovePath runFive = moveUsing(5);
        MovePath runEight = moveUsing(8);
        MovePath sprintTen = moveUsing(10);

        assertEquals(List.of(runFive, runEight), princess.getUnitOrdersFollower().limitToFormationPace(grasshopper,
              List.of(runFive, runEight, sprintTen)));
        assertEquals(List.of(runFive), princess.getUnitOrdersFollower().limitToFormationPace(longbow,
              List.of(runFive, runEight, sprintTen)));
    }

    @Test
    void aFormationBrokenOnContactIsNotPaced() {
        BipedMek leader = member(20, LEADER_HEX, 0, 6);
        member(21, new Coords(16, 25), 1, 3);
        Entity enemy = mock(Entity.class);
        when(enemy.getPosition()).thenReturn(LEADER_HEX.translated(0, 5));
        when(enemy.getBoardId()).thenReturn(0);
        enemies.add(enemy);
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
        // the leader faces north when stopped, so Echelon Left steps back to the south-west of the waypoint
        Coords slot = waypoint.translated(SOUTH_WEST, 2);
        assertEquals(Optional.of(slot), princess.getUnitOrdersFollower().getFormationSlot(firestarter));

        wolverine.setPosition(waypoint);
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
        member(20, LEADER_HEX, 0, 3);
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
}
