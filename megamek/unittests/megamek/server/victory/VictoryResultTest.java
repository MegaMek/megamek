package megamek.server.victory;

import megamek.common.Game;
import megamek.common.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VictoryResultTest {

    @Test
    public void testGetWinningPlayer() {
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
    public void testGetWinningTeam() {
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
    public void testProcessVictory() {
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
    public void testGetPlayerScoreNull() {
        VictoryResult victoryResult = new VictoryResult(true);

        assertEquals(0.0, victoryResult.getPlayerScore(1), 0.0);
    }

    @Test
    public void testGetPlayerScore() {
        VictoryResult victoryResult = new VictoryResult(true);
        victoryResult.setPlayerScore(1, 3);

        assertEquals(3.0, victoryResult.getPlayerScore(1), 0.0);
    }

    @Test
    public void testUpdateHiScore_Player() {
        VictoryResult victoryResult = new VictoryResult(false);
        victoryResult.setPlayerScore(1, 1);
        victoryResult.setPlayerScore(2, 2);
        victoryResult.setPlayerScore(3, 1);

        assertEquals(2, victoryResult.getWinningPlayer());
    }

    @Test
    public void testUpdateHiScore_Team() {
        VictoryResult victoryResult = new VictoryResult(false);
        victoryResult.setTeamScore(1, 1);
        victoryResult.setTeamScore(2, 2);
        victoryResult.setTeamScore(3, 1);

        assertEquals(2, victoryResult.getWinningTeam());
    }

    @Test
    public void testSetPlayerScore() {
        VictoryResult victoryResult = new VictoryResult(true);
        victoryResult.setPlayerScore(1, 3);

        assertEquals(1, victoryResult.getScoringPlayers().size());
        assertEquals(3.0, victoryResult.getPlayerScore(1), 0.0);
    }

    @Test
    public void testSetTeamScore() {
        VictoryResult victoryResult = new VictoryResult(true);
        victoryResult.setTeamScore(1, 3);

        assertEquals(1, victoryResult.getScoringTeams().size());
        assertEquals(3.0, victoryResult.getTeamScore(1), 0.0);
    }

}
