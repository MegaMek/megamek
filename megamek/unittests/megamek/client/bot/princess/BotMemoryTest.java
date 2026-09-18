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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.client.bot.princess.UnitMemory.MoveRecord;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the bot memory keeps from one turn to the next, and what it lets go of.
 */
@DisplayName("Bot memory")
class BotMemoryTest {

    private static final int UNIT_ID = 7;

    private BotMemory memory;

    @BeforeEach
    void beforeEach() {
        memory = new BotMemory();
    }

    private static MovePath pathFor(int unitId, Coords from, Coords to) {
        Entity entity = mock(Entity.class);
        when(entity.getId()).thenReturn(unitId);
        when(entity.getDisplayName()).thenReturn("Unit " + unitId);
        MovePath path = mock(MovePath.class);
        when(path.getEntity()).thenReturn(entity);
        when(path.getStartCoords()).thenReturn(from);
        when(path.getFinalCoords()).thenReturn(to);
        return path;
    }

    @Test
    @DisplayName("A unit's last move can be asked for on a later turn")
    void remembersTheLastMove() {
        assertNull(memory.unit(UNIT_ID).lastMove(), "nothing is remembered before the first move");

        memory.rememberMove(pathFor(UNIT_ID, new Coords(1, 1), new Coords(4, 2)), 3, BehaviorType.MoveToContact);

        MoveRecord lastMove = memory.unit(UNIT_ID).lastMove();
        assertNotNull(lastMove);
        assertEquals(3, lastMove.round());
        assertEquals(new Coords(1, 1), lastMove.from());
        assertEquals(new Coords(4, 2), lastMove.to());
        assertEquals(BehaviorType.MoveToContact, lastMove.behavior());
        assertFalse(lastMove.stayedPut());
        assertEquals(lastMove, memory.unit(UNIT_ID).moveInRound(3));
        assertNull(memory.unit(UNIT_ID).moveInRound(2), "the unit did not move in round 2");
    }

    @Test
    @DisplayName("Two paths in one round are one record: first starting hex, last ending hex")
    void aSecondPathInTheSameRoundExtendsTheFirst() {
        memory.rememberMove(pathFor(UNIT_ID, new Coords(1, 1), new Coords(2, 2)), 5, BehaviorType.Engaged);
        memory.rememberMove(pathFor(UNIT_ID, new Coords(2, 2), new Coords(1, 1)), 5, BehaviorType.Engaged);

        List<MoveRecord> moves = memory.unit(UNIT_ID).recentMoves();
        assertEquals(1, moves.size());
        assertEquals(new Coords(1, 1), moves.getFirst().from());
        assertEquals(new Coords(1, 1), moves.getFirst().to());
        assertTrue(moves.getFirst().stayedPut(), "out and back in one round is a round spent in place");
    }

    @Test
    @DisplayName("Only the last few rounds are kept, oldest dropped first")
    void historyIsBounded() {
        int roundsPlayed = UnitMemory.MOVES_REMEMBERED + 3;
        for (int round = 1; round <= roundsPlayed; round++) {
            memory.rememberMove(pathFor(UNIT_ID, new Coords(round, 1), new Coords(round + 1, 1)), round, null);
        }

        List<MoveRecord> moves = memory.unit(UNIT_ID).recentMoves();
        assertEquals(UnitMemory.MOVES_REMEMBERED, moves.size());
        assertEquals(roundsPlayed - UnitMemory.MOVES_REMEMBERED + 1, moves.getFirst().round());
        assertEquals(roundsPlayed, moves.getLast().round());
    }

    @Test
    @DisplayName("A unit that has left the game is forgotten; one still in it is not")
    void forgetsUnitsNoLongerInTheGame() {
        int goneUnitId = 8;
        memory.rememberMove(pathFor(UNIT_ID, new Coords(1, 1), new Coords(2, 1)), 1, null);
        memory.rememberMove(pathFor(goneUnitId, new Coords(5, 5), new Coords(6, 5)), 1, null);
        Game game = mock(Game.class);
        when(game.getEntity(UNIT_ID)).thenReturn(mock(Entity.class));
        when(game.getEntity(goneUnitId)).thenReturn(null);

        memory.forgetUnitsNoLongerInGame(game);

        assertNotNull(memory.unit(UNIT_ID).lastMove());
        assertNull(memory.unit(goneUnitId).lastMove(), "the page was dropped, so asking again starts an empty one");
    }

    @Test
    @DisplayName("The crippled list handed out is a snapshot that a later refresh does not change")
    void crippledSnapshotIsIndependent() {
        memory.setCrippledUnits(Set.of(UNIT_ID));
        Set<Integer> crippledAtStartOfTurn = memory.crippledUnitIds();

        memory.setCrippledUnits(Set.of());

        assertTrue(crippledAtStartOfTurn.contains(UNIT_ID), "end-of-turn honour checks judge the turn's start");
        assertFalse(memory.isCrippled(UNIT_ID));
    }

    @Test
    @DisplayName("Being shot while fleeing is news once, and stays known")
    void attackedWhileFleeingIsRememberedOnce() {
        assertFalse(memory.wasAttackedWhileFleeing(UNIT_ID));
        assertTrue(memory.rememberAttackedWhileFleeing(UNIT_ID), "the first report is news");
        assertFalse(memory.rememberAttackedWhileFleeing(UNIT_ID), "the second is not");
        assertTrue(memory.wasAttackedWhileFleeing(UNIT_ID));
    }

    @Test
    @DisplayName("A unit scooting to the fallback hex keeps going until it arrives or the order is dropped")
    void scootingIsRememberedUntilCleared() {
        memory.rememberScooting(UNIT_ID);
        memory.rememberScooting(UNIT_ID + 1);
        assertTrue(memory.isScooting(UNIT_ID));

        memory.forgetScooting(UNIT_ID);
        assertFalse(memory.isScooting(UNIT_ID));
        assertTrue(memory.isScooting(UNIT_ID + 1));

        memory.forgetAllScooting();
        assertFalse(memory.isScooting(UNIT_ID + 1));
    }
}
