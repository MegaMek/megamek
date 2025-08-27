/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.server.victory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import megamek.client.ui.util.PlayerColour;
import megamek.common.Player;
import megamek.common.force.Forces;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.server.totalwarfare.TWGameManager;
import org.junit.jupiter.api.Test;

class GameManagerTest {

    protected Game createMockedGame() {
        Game testGame = mock(Game.class);
        Forces testForces = new Forces(testGame);
        when(testGame.getGameListeners()).thenReturn(new Vector<>());
        when(testGame.getEntitiesVector()).thenReturn(Collections.emptyList());
        when(testGame.getPlayersList()).thenReturn(Collections.emptyList());
        when(testGame.getAttacks()).thenReturn(Collections.emptyEnumeration());
        when(testGame.getAttacksVector()).thenReturn(new Vector<>());
        when(testGame.getForces()).thenReturn(testForces);
        when(testGame.getOptions()).thenReturn(new GameOptions());
        return testGame;
    }

    @Test
    void testVictory() {
        TWGameManager gameManager = new TWGameManager();
        VictoryResult testVictoryResultFalse = new VictoryResult(false);
        VictoryResult testVictoryResultTrue = new VictoryResult(true);

        Game testGame = createMockedGame();

        // test whether the server.victory() returns false when mocking VictoryResult as
        // false
        when(testGame.getVictoryResult()).thenReturn(testVictoryResultFalse);
        gameManager.setGame(testGame);
        assertFalse(gameManager.victory());

        // test whether the server.victory() returns true when mocking VictoryResult as
        // true
        when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);
        gameManager.setGame(testGame);
        assertTrue(gameManager.victory());
    }

    @Test
    void testVictoryDrawReport() {
        TWGameManager gameManager = new TWGameManager();
        VictoryResult testVictoryResultTrue = new VictoryResult(true);
        Game testGame = createMockedGame();
        when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);

        gameManager.setGame(testGame);
        gameManager.victory();
        verify(testGame, times(1)).setVictoryPlayerId(Player.PLAYER_NONE);
        verify(testGame, times(1)).setVictoryTeam(Player.TEAM_NONE);
    }

    @Test
    void testVictoryFalseReport() {
        TWGameManager gameManager = new TWGameManager();
        VictoryResult testVictoryResultTrue = new VictoryResult(false);
        Game testGame = createMockedGame();
        when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);

        gameManager.setGame(testGame);
        gameManager.victory();
        verify(testGame, times(1)).cancelVictory();
    }

    @Test
    void testCancelVictory() {
        TWGameManager gameManager = new TWGameManager();
        VictoryResult testVictoryResultTrue = new VictoryResult(false);
        Game testGame = createMockedGame();
        when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);
        when(testGame.isForceVictory()).thenReturn(true);

        gameManager.setGame(testGame);
        gameManager.victory();
        verify(testGame, times(1)).cancelVictory();
    }

    @Test
    void testVictoryWinReports() {
        TWGameManager gameManager = new TWGameManager();

        int winner = 1;

        // Mock a win victory result
        // Only 1 report should be generated as the team is set to TEAM_NONE
        Game testGame = createMockedGame();
        VictoryResult victoryResult = mock(VictoryResult.class);
        when(victoryResult.processVictory(testGame)).thenCallRealMethod();
        when(victoryResult.getReports()).thenReturn(new ArrayList<>());
        when(victoryResult.isVictory()).thenReturn(true);
        when(victoryResult.isDraw()).thenReturn(false);
        when(victoryResult.getWinningPlayer()).thenReturn(winner);
        when(victoryResult.getWinningTeam()).thenReturn(Player.TEAM_NONE);

        Player mockedPlayer = mock(Player.class);
        when(mockedPlayer.getName()).thenReturn("The champion");
        when(mockedPlayer.getColour()).thenReturn(PlayerColour.BLUE);

        when(testGame.getVictoryResult()).thenReturn(victoryResult);
        when(testGame.getPlayer(winner)).thenReturn(mockedPlayer);

        gameManager.setGame(testGame);
        gameManager.victory();

        assertSame(1, gameManager.getMainPhaseReport().size());

        // Second test server tests with both a team != TEAM_NONE and a player !=
        // PLAYER_NONE
        // Two reports should be generated
        TWGameManager gameManager2 = new TWGameManager();

        when(victoryResult.getWinningTeam()).thenReturn(10);
        when(victoryResult.getReports()).thenReturn(new ArrayList<>());
        gameManager2.setGame(testGame);
        gameManager2.victory();

        assertSame(2, gameManager2.getMainPhaseReport().size());
    }
}
