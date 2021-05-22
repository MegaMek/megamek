package megamek.common;

import junit.framework.TestCase;
import megamek.server.victory.VictoryResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GameTest {

    @Test
    public void testCancelVictory() {
        // Default test
        IGame game = new Game();
        game.cancelVictory();
        TestCase.assertFalse(game.isForceVictory());
        TestCase.assertSame(IPlayer.PLAYER_NONE, game.getVictoryPlayerId());
        TestCase.assertSame(IPlayer.TEAM_NONE, game.getVictoryTeam());

        // Test with members set to specific values
        IGame game2 = new Game();
        game2.setVictoryPlayerId(10);
        game2.setVictoryTeam(10);
        game2.setForceVictory(true);

        game2.cancelVictory();
        TestCase.assertFalse(game.isForceVictory());
        TestCase.assertSame(IPlayer.PLAYER_NONE, game.getVictoryPlayerId());
        TestCase.assertSame(IPlayer.TEAM_NONE, game.getVictoryTeam());
    }

    @Test
    public void testGetVictoryReport() {
        IGame game = new Game();
        game.createVictoryConditions();
        VictoryResult victoryResult = game.getVictoryResult();
        TestCase.assertNotNull(victoryResult);

        // Note: this accessors are tested in VictoryResultTest
        TestCase.assertSame(IPlayer.PLAYER_NONE, victoryResult.getWinningPlayer());
        TestCase.assertSame(IPlayer.TEAM_NONE, victoryResult.getWinningTeam());

        int winningPlayer = 2;
        int winningTeam = 5;

        // Test an actual scenario
        IGame game2 = new Game();
        game2.setVictoryTeam(winningTeam);
        game2.setVictoryPlayerId(winningPlayer);
        game2.setForceVictory(true);
        game2.createVictoryConditions();
        VictoryResult victoryResult2 = game2.getVictoryResult();

        TestCase.assertSame(winningPlayer, victoryResult2.getWinningPlayer());
        TestCase.assertSame(winningTeam, victoryResult2.getWinningTeam());
    }

}
