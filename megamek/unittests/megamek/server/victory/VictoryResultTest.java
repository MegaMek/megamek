package megamek.server.victory;

import junit.framework.TestCase;
import megamek.common.IPlayer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class VictoryResultTest {

    @Test
    public void testGetWinningPlayer() {
        // Trivial case: no players
        VictoryResult testResult = new VictoryResult(false);
        TestCase.assertSame(IPlayer.PLAYER_NONE, testResult.getWinningPlayer());

        // Case with two players
        int winningPlayer = 0;
        int losingPlayer = 1;

        testResult.addPlayerScore(winningPlayer, 100);
        testResult.addPlayerScore(losingPlayer, 40);

        TestCase.assertSame(winningPlayer, testResult.getWinningPlayer());

        // Case with three players and a draw
        int secondWinningPlayer = 2;

        testResult.addPlayerScore(secondWinningPlayer, 100);
        TestCase.assertNotSame(secondWinningPlayer, testResult.getWinningPlayer());
        TestCase.assertNotSame(winningPlayer, testResult.getWinningPlayer());
        TestCase.assertSame(IPlayer.PLAYER_NONE, testResult.getWinningPlayer());
    }

    @Test
    public void testGetWinningTeam() {
        // Trivial case: no team
        VictoryResult testResult = new VictoryResult(false);
        TestCase.assertSame(IPlayer.TEAM_NONE, testResult.getWinningTeam());

        // Case with two teams
        int winningTeam = 1;
        int losingTeam = 2;

        testResult.addTeamScore(winningTeam, 100);
        testResult.addTeamScore(losingTeam, 40);

        TestCase.assertSame(winningTeam, testResult.getWinningTeam());

        // Case with three teams and a draw
        int secondWinningTeam = 3;

        testResult.addTeamScore(secondWinningTeam, 100);
        TestCase.assertNotSame(secondWinningTeam, testResult.getWinningTeam());
        TestCase.assertNotSame(winningTeam, testResult.getWinningTeam());
        TestCase.assertSame(IPlayer.TEAM_NONE, testResult.getWinningTeam());
    }
}
