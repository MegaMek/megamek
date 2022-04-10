package megamek.server.victory;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.force.Forces;
import megamek.common.options.GameOptions;
import megamek.server.Server;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(value = JUnit4.class)
public class ServerTest {

    protected Game createMockedGame() {
        Game testGame = Mockito.mock(Game.class);
        Forces testForces = new Forces(testGame);
        Mockito.when(testGame.getGameListeners()).thenReturn(new Vector<>());
        Mockito.when(testGame.getEntities()).thenReturn(Collections.emptyIterator());
        Mockito.when(testGame.getPlayers()).thenReturn(Collections.emptyEnumeration());
        Mockito.when(testGame.getAttacks()).thenReturn(Collections.emptyEnumeration());
        Mockito.when(testGame.getForces()).thenReturn(testForces);
        Mockito.when(testGame.getOptions()).thenReturn(new GameOptions());
        return testGame;
    }


    @Test
    public void testVictory() throws IOException {
        Server testServer = new Server("test", 0);
        VictoryResult testVictoryResultFalse = new VictoryResult(false);
        VictoryResult testVictoryResultTrue = new VictoryResult(true);

        Game testGame = createMockedGame();
        //test whether the server.victory() returns false when mocking VictoryResult as false
        Mockito.when(testGame.getVictoryResult()).thenReturn(testVictoryResultFalse);
        testServer.setGame(testGame);
        assertFalse(VictoryChecker.victory(testServer));
        //test whether the server.victory() returns true when mocking VictoryResult as true
        Mockito.when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);
        testServer.setGame(testGame);
        assertTrue(VictoryChecker.victory(testServer));
    }

    @Test
    public void testVictoryDrawReport() throws IOException {
        Server testServer = new Server("test", 0);
        VictoryResult testVictoryResultTrue = new VictoryResult(true);
        Game testGame = createMockedGame();
        Mockito.when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);
        testServer.setGame(testGame);
        VictoryChecker.victory(testServer);
        Mockito.verify(testGame, Mockito.times(1)).setVictoryPlayerId(Player.PLAYER_NONE);
        Mockito.verify(testGame, Mockito.times(1)).setVictoryTeam(Player.TEAM_NONE);
    }

    @Test
    public void testVictoryFalseReport() throws IOException {
        Server testServer = new Server("test", 0);
        VictoryResult testVictoryResultTrue = new VictoryResult(false);
        Game testGame = createMockedGame();
        Mockito.when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);

        testServer.setGame(testGame);
        VictoryChecker.victory(testServer);
        Mockito.verify(testGame, Mockito.times(1)).cancelVictory();
    }

    @Test
    public void testCancelVictory() throws IOException {
        Server testServer = new Server("test", 0);
        VictoryResult testVictoryResultTrue = new VictoryResult(false);
        Game testGame = createMockedGame();
        Mockito.when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);
        Mockito.when(testGame.isForceVictory()).thenReturn(true);


        testServer.setGame(testGame);
        VictoryChecker.victory(testServer);
        Mockito.verify(testGame, Mockito.times(1)).cancelVictory();

    }

    @Test
    public void testVictoryWinReports() throws IOException {
        Server testServer = new Server("test", 0);

        int winner = 1;

        // Mock a win victory result
        // Only 1 report should be generated as the team is set to TEAM_NONE
        Game testGame = createMockedGame();
        VictoryResult victoryResult = Mockito.mock(VictoryResult.class);
        Mockito.when(victoryResult.processVictory(testGame)).thenCallRealMethod();
        Mockito.when(victoryResult.getReports()).thenReturn(new ArrayList<>());
        Mockito.when(victoryResult.victory()).thenReturn(true);
        Mockito.when(victoryResult.isDraw()).thenReturn(false);
        Mockito.when(victoryResult.getWinningPlayer()).thenReturn(winner);
        Mockito.when(victoryResult.getWinningTeam()).thenReturn(Player.TEAM_NONE);

        Player mockedPlayer = Mockito.mock(Player.class);
        Mockito.when(mockedPlayer.getName()).thenReturn("The champion");
        Mockito.when(mockedPlayer.getColour()).thenReturn(PlayerColour.BLUE);

        Mockito.when(testGame.getVictoryResult()).thenReturn(victoryResult);
        Mockito.when(testGame.getPlayer(winner)).thenReturn(mockedPlayer);

        testServer.setGame(testGame);
        VictoryChecker.victory(testServer);

        assertSame(1, testServer.getvPhaseReport().size());

        // Second test server tests with both a team != TEAM_NONE and a player != PLAYER_NONE
        // Two reports should be generated
        Server testServer2 = new Server("test", 0);

        Mockito.when(victoryResult.getWinningTeam()).thenReturn(10);
        Mockito.when(victoryResult.getReports()).thenReturn(new ArrayList<>());
        testServer2.setGame(testGame);
        VictoryChecker.victory(testServer2);

        assertSame(2, testServer2.getvPhaseReport().size());
    }

}


