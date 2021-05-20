package megamek.server.victory;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.IPlayer;
import megamek.server.Server;
import junit.framework.TestCase;
import megamek.common.IGame;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;


@RunWith(JUnit4.class)
public class ServerTest {

    protected IGame createMockedGame() {
        IGame testGame = Mockito.mock(IGame.class);
        Mockito.when(testGame.getGameListeners()).thenReturn(new Vector<>());
        Mockito.when(testGame.getEntities()).thenReturn(Collections.emptyIterator());
        Mockito.when(testGame.getPlayers()).thenReturn(Collections.emptyEnumeration());
        Mockito.when(testGame.getAttacks()).thenReturn(Collections.emptyEnumeration());
        return testGame;
    }


    @Test
    public void testVictory() throws IOException {
        Server testServer = new Server("test", 123);
        VictoryResult testVictoryResultFalse = new VictoryResult(false);
        VictoryResult testVictoryResultTrue = new VictoryResult(true);

        IGame testGame = createMockedGame();

        //test whether the server.victory() returns false when mocking VictoryResult as false
        Mockito.when(testGame.getVictoryResult()).thenReturn(testVictoryResultFalse);
        testServer.setGame(testGame);
        TestCase.assertFalse(testServer.victory());

        //test whether the server.victory() returns true when mocking VictoryResult as true
        Mockito.when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);
        testServer.setGame(testGame);
        TestCase.assertTrue(testServer.victory());
    }

    @Test
    public void testVictoryDrawReport() throws IOException {
        Server testServer = new Server("test", 4);
        VictoryResult testVictoryResultTrue = new VictoryResult(true);
        IGame testGame = createMockedGame();
        Mockito.when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);

        testServer.setGame(testGame);
        testServer.victory();
        Mockito.verify(testGame, Mockito.times(1)).setVictoryPlayerId(IPlayer.PLAYER_NONE);
        Mockito.verify(testGame, Mockito.times(1)).setVictoryTeam(IPlayer.TEAM_NONE);
    }

    @Test
    public void testVictoryFalseReport() throws IOException {
        Server testServer = new Server("test", 8);
        VictoryResult testVictoryResultTrue = new VictoryResult(false);
        IGame testGame = createMockedGame();
        Mockito.when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);

        testServer.setGame(testGame);
        testServer.victory();
        Mockito.verify(testGame, Mockito.times(1)).setVictoryPlayerId(IPlayer.PLAYER_NONE);
        Mockito.verify(testGame, Mockito.times(1)).setVictoryTeam(IPlayer.TEAM_NONE);
        Mockito.verify(testGame, Mockito.times(1)).isForceVictory();

    }

    @Test
    public void testCancelVictory() throws IOException {
        Server testServer = new Server("test", 8);
        VictoryResult testVictoryResultTrue = new VictoryResult(false);
        IGame testGame = createMockedGame();
        Mockito.when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);
        Mockito.when(testGame.isForceVictory()).thenReturn(true);


        testServer.setGame(testGame);
        testServer.victory();
        Mockito.verify(testGame, Mockito.times(2)).setVictoryPlayerId(IPlayer.PLAYER_NONE);
        Mockito.verify(testGame, Mockito.times(2)).setVictoryTeam(IPlayer.TEAM_NONE);
        Mockito.verify(testGame, Mockito.times(1)).setForceVictory(false);
    }

    @Test
    public void testVictoryWinReports() throws IOException {
        Server testServer = new Server("test", 0);

        int winner = 1;

        // Mock a win victory result
        // Only 1 report should be generated as the team is set to TEAM_NONE
        VictoryResult victoryResult = Mockito.mock(VictoryResult.class);
        Mockito.when(victoryResult.getReports()).thenReturn(new ArrayList<>());
        Mockito.when(victoryResult.victory()).thenReturn(true);
        Mockito.when(victoryResult.isDraw()).thenReturn(false);
        Mockito.when(victoryResult.getWinningPlayer()).thenReturn(winner);
        Mockito.when(victoryResult.getWinningTeam()).thenReturn(IPlayer.TEAM_NONE);

        IPlayer mockedPlayer = Mockito.mock(IPlayer.class);
        Mockito.when(mockedPlayer.getName()).thenReturn("The champion");
        Mockito.when(mockedPlayer.getColour()).thenReturn(PlayerColour.BLUE);

        IGame testGame = createMockedGame();
        Mockito.when(testGame.getVictoryResult()).thenReturn(victoryResult);
        Mockito.when(testGame.getPlayer(winner)).thenReturn(mockedPlayer);

        testServer.setGame(testGame);
        testServer.victory();

        TestCase.assertSame(1, testServer.getvPhaseReport().size());

        // Second test server tests with both a team != TEAM_NONE and a player != PLAYER_NONE
        // Two reports should be generated
        Server testServer2 = new Server("test", 0);

        Mockito.when(victoryResult.getWinningTeam()).thenReturn(10);
        testServer2.setGame(testGame);
        testServer2.victory();

        TestCase.assertSame(2, testServer2.getvPhaseReport().size());
    }

}


