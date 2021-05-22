package megamek.common;

import junit.framework.TestCase;
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

    }

}
