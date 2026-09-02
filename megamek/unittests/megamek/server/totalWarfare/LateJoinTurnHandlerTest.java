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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.game.InGameObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A scenario has no lobby, so the pre-game player-turn phases build their turn order before the other
 * human players have connected. The late joiner must be given the turn they would have had.
 */
class LateJoinTurnHandlerTest {

    private Game game;
    private LateJoinTurnHandler handler;
    private Player host;
    private Player lateJoiner;
    private List<GameTurn> turns;
    private List<InGameObject> units;

    @BeforeEach
    void setUp() {
        game = mock(Game.class);
        TWGameManager gameManager = mock(TWGameManager.class);
        when(gameManager.getGame()).thenReturn(game);
        handler = new LateJoinTurnHandler(gameManager);

        host = new Player(0, "Host");
        lateJoiner = new Player(1, "Second");

        // the turn order as the phase built it: the host alone, index sitting on that turn
        turns = new ArrayList<>(List.of(new GameTurn(host.getId())));
        when(game.getTurnsList()).thenReturn(turns);
        when(game.getTurnIndex()).thenReturn(0);
        doAnswer(invocation -> {
            turns.add(invocation.<Integer>getArgument(1) + 1, invocation.getArgument(0));
            return null;
        }).when(game).insertTurnAfter(any(GameTurn.class), anyInt());

        // getEntitiesOwnedBy is an interface default, which a mock does not run - stub the count itself
        units = new ArrayList<>();
        when(game.getEntitiesOwnedBy(any(Player.class))).thenAnswer(invocation -> {
            Player owner = invocation.getArgument(0);
            return (int) units.stream().filter(unit -> unit.getOwnerId() == owner.getId()).count();
        });
    }

    private void unitOwnedBy(Player owner) {
        InGameObject unit = mock(InGameObject.class);
        when(unit.getOwnerId()).thenReturn(owner.getId());
        units.add(unit);
    }

    @Test
    void testALatePlayerWithUnitsGetsTheNextTurnInTheVictorySetupPhase() {
        when(game.getPhase()).thenReturn(GamePhase.VICTORY_SETUP);
        unitOwnedBy(host);
        unitOwnedBy(lateJoiner);

        assertTrue(handler.giveTurnIfPhaseHasPassedThemBy(lateJoiner));

        assertEquals(2, turns.size());
        assertEquals(lateJoiner.getId(), turns.get(1).playerId(), "they act right after the current turn");
    }

    @Test
    void testTheOtherPreGamePlayerTurnPhasesAreCoveredToo() {
        unitOwnedBy(lateJoiner);
        for (GamePhase phase : List.of(GamePhase.SET_ARTILLERY_AUTO_HIT_HEXES, GamePhase.DEPLOY_MINEFIELDS)) {
            turns.clear();
            turns.add(new GameTurn(host.getId()));
            when(game.getPhase()).thenReturn(phase);

            assertTrue(handler.giveTurnIfPhaseHasPassedThemBy(lateJoiner), phase + " builds its order at start too");
        }
    }

    @Test
    void testAPlayerWithNoUnitsGetsNoTurn() {
        when(game.getPhase()).thenReturn(GamePhase.VICTORY_SETUP);
        unitOwnedBy(host);

        // the phase skips unit-less players deliberately - an observer must not gain a turn by connecting late
        assertFalse(handler.giveTurnIfPhaseHasPassedThemBy(lateJoiner));
        assertEquals(1, turns.size());
    }

    @Test
    void testAPlayerWhoAlreadyHasATurnComingIsNotGivenASecond() {
        when(game.getPhase()).thenReturn(GamePhase.VICTORY_SETUP);
        unitOwnedBy(lateJoiner);
        turns.add(new GameTurn(lateJoiner.getId()));

        assertFalse(handler.giveTurnIfPhaseHasPassedThemBy(lateJoiner));
        assertEquals(2, turns.size());
    }

    @Test
    void testNothingHappensOutsideThePreGamePlayerTurnPhases() {
        unitOwnedBy(lateJoiner);
        for (GamePhase phase : List.of(GamePhase.LOUNGE, GamePhase.DEPLOYMENT, GamePhase.MOVEMENT,
              GamePhase.FIRING, GamePhase.END)) {
            when(game.getPhase()).thenReturn(phase);
            assertFalse(handler.giveTurnIfPhaseHasPassedThemBy(lateJoiner),
                  phase + " already handles late joiners its own way");
        }
        assertEquals(1, turns.size());
    }
}
