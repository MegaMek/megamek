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
package megamek.server.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The Skip Current Turn button sends /skip. Before the Game Master could skip any turn, the button only worked on a
 * turn whose player had disconnected, so a Game Master clicking it during a live player's turn got "skip failed".
 */
class SkipCommandTest {

    /**
     * @param turnIsStuck Whether the current turn's player has disconnected or has nothing to move
     *
     * @return a skip command on a game that is in the middle of a turn
     */
    private static SkipCommand commandDuringATurn(boolean turnIsStuck) {
        Game game = Mockito.mock(Game.class);
        Mockito.when(game.getTurn()).thenReturn(Mockito.mock(GameTurn.class));
        TWGameManager gameManager = Mockito.mock(TWGameManager.class);
        Mockito.when(gameManager.getGame()).thenReturn(game);
        Mockito.when(gameManager.isTurnSkippable()).thenReturn(turnIsStuck);
        return new SkipCommand(Mockito.mock(Server.class), gameManager);
    }

    private static Player playerWithGameMasterRole(boolean holdsTheRole) {
        Player player = new Player(0, "Issuer");
        player.setGameMaster(holdsTheRole);
        return player;
    }

    @Test
    void aGameMasterMaySkipALivePlayersTurn() {
        assertTrue(commandDuringATurn(false).canSkip(playerWithGameMasterRole(true)),
              "the Game Master runs the game, so the Skip Current Turn button must work for them on any turn");
    }

    @Test
    void anOrdinaryPlayerMayNotSkipALivePlayersTurn() {
        assertFalse(commandDuringATurn(false).canSkip(playerWithGameMasterRole(false)),
              "skipping somebody who is still playing would let any player take their turn away");
    }

    @Test
    void anyPlayerMaySkipAStuckTurn() {
        assertTrue(commandDuringATurn(true).canSkip(playerWithGameMasterRole(false)),
              "a disconnected player's turn must be skippable by anybody, or the game hangs");
    }

    @Test
    void nothingIsSkippedWhenNoTurnIsRunning() {
        TWGameManager gameManager = Mockito.mock(TWGameManager.class);
        Mockito.when(gameManager.getGame()).thenReturn(Mockito.mock(Game.class));
        SkipCommand command = new SkipCommand(Mockito.mock(Server.class), gameManager);

        assertFalse(command.canSkip(playerWithGameMasterRole(true)), "between turns there is nothing to skip");
    }
}
