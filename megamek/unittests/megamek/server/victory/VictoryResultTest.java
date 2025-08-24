/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.game.Game;
import megamek.common.Player;
import org.junit.jupiter.api.Test;

class VictoryResultTest {

    @Test
    void testGetWinningPlayer() {
        // Trivial case: no players
        VictoryResult testResult = new VictoryResult(false);
        assertSame(Player.PLAYER_NONE, testResult.getWinningPlayer());

        // Case with two players
        int winningPlayer = 0;
        int losingPlayer = 1;

        testResult.setPlayerScore(winningPlayer, 100);
        testResult.setPlayerScore(losingPlayer, 40);

        assertSame(winningPlayer, testResult.getWinningPlayer());

        // Case with three players and a draw
        int secondWinningPlayer = 2;

        testResult.setPlayerScore(secondWinningPlayer, 100);
        assertNotSame(secondWinningPlayer, testResult.getWinningPlayer());
        assertNotSame(winningPlayer, testResult.getWinningPlayer());
        assertSame(Player.PLAYER_NONE, testResult.getWinningPlayer());
    }

    @Test
    void testGetWinningTeam() {
        // Trivial case: no team
        VictoryResult testResult = new VictoryResult(false);
        assertSame(Player.TEAM_NONE, testResult.getWinningTeam());

        // Case with two teams
        int winningTeam = 1;
        int losingTeam = 2;

        testResult.setTeamScore(winningTeam, 100);
        testResult.setTeamScore(losingTeam, 40);

        assertSame(winningTeam, testResult.getWinningTeam());

        // Case with three teams and a draw
        int secondWinningTeam = 3;

        testResult.setTeamScore(secondWinningTeam, 100);
        assertNotSame(secondWinningTeam, testResult.getWinningTeam());
        assertNotSame(winningTeam, testResult.getWinningTeam());
        assertSame(Player.TEAM_NONE, testResult.getWinningTeam());
    }

    @Test
    void testProcessVictory() {
        // Trivial cases
        VictoryResult victoryResult = new VictoryResult(true);

        Player playerMock = mock(Player.class);
        when(playerMock.getColorForPlayer()).thenReturn("");

        Game gameMock = mock(Game.class);
        when(gameMock.getPlayer(anyInt())).thenReturn(playerMock);

        assertTrue(victoryResult.processVictory(gameMock).isEmpty());

        VictoryResult victoryResult2 = new VictoryResult(false);
        assertTrue(victoryResult2.processVictory(gameMock).isEmpty());

        // Less trivial cases
        // Only won player is set
        VictoryResult victoryResult3 = new VictoryResult(true);
        victoryResult3.setPlayerScore(1, 100);
        assertSame(1, victoryResult3.processVictory(gameMock).size());

        // Only won team is set
        VictoryResult victoryResult4 = new VictoryResult(true);
        victoryResult4.setTeamScore(1, 100);
        assertSame(1, victoryResult4.processVictory(gameMock).size());

        // Both player and team winners are set
        VictoryResult victoryResult5 = new VictoryResult(true);
        victoryResult5.setPlayerScore(1, 100);
        victoryResult5.setTeamScore(1, 100);
        assertSame(2, victoryResult5.processVictory(gameMock).size());

        // Draw result
        VictoryResult victoryResult6 = new VictoryResult(true);
        victoryResult6.setPlayerScore(1, 100);
        victoryResult6.setPlayerScore(2, 100);
        assertTrue(victoryResult6.processVictory(gameMock).isEmpty());
    }

    @Test
    void testGetPlayerScoreNull() {
        VictoryResult victoryResult = new VictoryResult(true);

        assertEquals(0.0, victoryResult.getPlayerScore(1), 0.0);
    }

    @Test
    void testGetPlayerScore() {
        VictoryResult victoryResult = new VictoryResult(true);
        victoryResult.setPlayerScore(1, 3);

        assertEquals(3.0, victoryResult.getPlayerScore(1), 0.0);
    }

    @Test
    void testUpdateHiScore_Player() {
        VictoryResult victoryResult = new VictoryResult(false);
        victoryResult.setPlayerScore(1, 1);
        victoryResult.setPlayerScore(2, 2);
        victoryResult.setPlayerScore(3, 1);

        assertEquals(2, victoryResult.getWinningPlayer());
    }

    @Test
    void testUpdateHiScore_Team() {
        VictoryResult victoryResult = new VictoryResult(false);
        victoryResult.setTeamScore(1, 1);
        victoryResult.setTeamScore(2, 2);
        victoryResult.setTeamScore(3, 1);

        assertEquals(2, victoryResult.getWinningTeam());
    }

    @Test
    void testSetPlayerScore() {
        VictoryResult victoryResult = new VictoryResult(true);
        victoryResult.setPlayerScore(1, 3);

        assertEquals(1, victoryResult.getScoringPlayers().size());
        assertEquals(3.0, victoryResult.getPlayerScore(1), 0.0);
    }

    @Test
    void testSetTeamScore() {
        VictoryResult victoryResult = new VictoryResult(true);
        victoryResult.setTeamScore(1, 3);

        assertEquals(1, victoryResult.getScoringTeams().size());
        assertEquals(3.0, victoryResult.getTeamScore(1), 0.0);
    }

}
