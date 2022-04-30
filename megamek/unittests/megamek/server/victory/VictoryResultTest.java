package megamek.server.victory;

import megamek.common.Game;
import megamek.common.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(value = JUnit4.class)
public class VictoryResultTest {

    @Test
    public void testGetWinningPlayer() {
        // Trivial case: no players
        VictoryResult testResult = new VictoryResult(false);
        assertSame(Player.PLAYER_NONE, testResult.getWinningPlayer());

        // Case with two players
        int winningPlayer = 0;
        int losingPlayer = 1;

        testResult.addPlayerScore(winningPlayer, 100);
        testResult.addPlayerScore(losingPlayer, 40);

        assertSame(winningPlayer, testResult.getWinningPlayer());

        // Case with three players and a draw
        int secondWinningPlayer = 2;

        testResult.addPlayerScore(secondWinningPlayer, 100);
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

        testResult.addTeamScore(winningTeam, 100);
        testResult.addTeamScore(losingTeam, 40);

        assertSame(winningTeam, testResult.getWinningTeam());

        // Case with three teams and a draw
        int secondWinningTeam = 3;

        testResult.addTeamScore(secondWinningTeam, 100);
        assertNotSame(secondWinningTeam, testResult.getWinningTeam());
        assertNotSame(winningTeam, testResult.getWinningTeam());
        assertSame(Player.TEAM_NONE, testResult.getWinningTeam());
    }

    @Test
    public void testProcessVictory() {
        // Trivial cases
        VictoryResult victoryResult = new VictoryResult(true);

        Player playerMock = Mockito.mock(Player.class);
        Mockito.when(playerMock.getColorForPlayer()).thenReturn("");

        Game gameMock = Mockito.mock(Game.class);
        Mockito.when(gameMock.getPlayer(Mockito.anyInt())).thenReturn(playerMock);

        assertTrue(victoryResult.processVictory(gameMock).isEmpty());

        VictoryResult victoryResult2 = new VictoryResult(false);
        assertTrue(victoryResult2.processVictory(gameMock).isEmpty());

        // Less trivial cases
        // Only won player is set
        VictoryResult victoryResult3 = new VictoryResult(true);
        victoryResult3.addPlayerScore(1, 100);
        assertSame(1, victoryResult3.processVictory(gameMock).size());

        // Only won team is set
        VictoryResult victoryResult4 = new VictoryResult(true);
        victoryResult4.addTeamScore(1, 100);
        assertSame(1, victoryResult4.processVictory(gameMock).size());

        // Both player and team winners are set
        VictoryResult victoryResult5 = new VictoryResult(true);
        victoryResult5.addPlayerScore(1, 100);
        victoryResult5.addTeamScore(1, 100);
        assertSame(2, victoryResult5.processVictory(gameMock).size());

        // Draw result
        VictoryResult victoryResult6 = new VictoryResult(true);
        victoryResult6.addPlayerScore(1, 100);
        victoryResult6.addPlayerScore(2, 100);
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
        victoryResult.addPlayerScore(1, 3);

        assertEquals(3.0, victoryResult.getPlayerScore(1), 0.0);
    }

    @Test
    public void testUpdateHiScore_Player() {
        VictoryResult victoryResult = new VictoryResult(false);
        victoryResult.addPlayerScore(1, 1);
        victoryResult.addPlayerScore(2, 2);
        victoryResult.addPlayerScore(3, 1);

        assertTrue(victoryResult.isWinningPlayer(2));
    }

    @Test
    public void testUpdateHiScore_Team() {
        VictoryResult victoryResult = new VictoryResult(false);
        victoryResult.addTeamScore(1, 1);
        victoryResult.addTeamScore(2, 2);
        victoryResult.addTeamScore(3, 1);

        assertTrue(victoryResult.isWinningTeam(2));
    }

    @Test
    public void testAddPlayerScore() {
        VictoryResult victoryResult = new VictoryResult(true);
        victoryResult.addPlayerScore(1, 3);

        assertEquals(1, victoryResult.getPlayers().length);
        assertEquals(3.0, victoryResult.getPlayerScore(1), 0.0);
    }

    @Test
    public void testAddTeamScore() {
        VictoryResult victoryResult = new VictoryResult(true);
        victoryResult.addTeamScore(1, 3);

        assertEquals(1, victoryResult.getTeams().length);
        assertEquals(3.0, victoryResult.getTeamScore(1), 0.0);
    }

    @Test
    public void testGetPlayers() {
        VictoryResult victoryResult = new VictoryResult(true);
        victoryResult.addPlayerScore(1, 3);
        victoryResult.addPlayerScore(2, 3);

        assertEquals(1, victoryResult.getPlayers()[0]);
        assertEquals(2, victoryResult.getPlayers()[1]);
    }
}
