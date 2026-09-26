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
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
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
    private static final int SOUTH_EAST = 2;
    private static final int SOUTH = 3;
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
    void aFollowerStandsInItsSlotBesideTheLeader() {
        member(20, LEADER_HEX, 0, 3);
        BipedMek second = member(21, new Coords(16, 25), 1, 4);

        // heading north toward the waypoint, the Echelon Right steps back south-east
        assertEquals(Optional.of(LEADER_HEX.translated(SOUTH_EAST, 2)),
              princess.getUnitOrdersFollower().getFormationSlot(second));
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
        Coords ideal = LEADER_HEX.translated(SOUTH_EAST, 2);
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
        // wall off everything south-east of the leader, leaving the column behind it open
        for (int x = LEADER_HEX.getX() + 1; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                board.getHex(x, y).addTerrain(new Terrain(
                      Terrains.IMPASSABLE, 1));
            }
        }

        assertEquals(Optional.of(LEADER_HEX.translated(SOUTH, 2)),
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

    @Test
    void theLeaderKeepsToTheSlowestUnitsWalk() {
        BipedMek leader = member(20, LEADER_HEX, 0, 6);
        member(21, new Coords(16, 25), 1, 3);
        MovePath walkThree = mock(MovePath.class);
        when(walkThree.getMpUsed()).thenReturn(3);
        MovePath runFive = mock(MovePath.class);
        when(runFive.getMpUsed()).thenReturn(5);

        List<MovePath> paced = princess.getUnitOrdersFollower().limitToFormationPace(leader,
              List.of(walkThree, runFive));

        assertEquals(List.of(walkThree), paced);
    }
}
