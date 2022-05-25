package megamek.server.victory;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.force.Forces;
import megamek.common.options.GameOptions;
import megamek.server.Server;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerTest {

    protected Game createMockedGame() {
        Game testGame = mock(Game.class);
        Forces testForces = new Forces(testGame);
        when(testGame.getGameListeners()).thenReturn(new Vector<>());
        when(testGame.getEntities()).thenReturn(Collections.emptyIterator());
        when(testGame.getPlayers()).thenReturn(Collections.emptyEnumeration());
        when(testGame.getAttacks()).thenReturn(Collections.emptyEnumeration());
        when(testGame.getForces()).thenReturn(testForces);
        when(testGame.getOptions()).thenReturn(new GameOptions());
        return testGame;
    }

    @Test
    public void testVictory() throws IOException {
        Server testServer = new Server("test", 0);
        VictoryResult testVictoryResultFalse = new VictoryResult(false);
        VictoryResult testVictoryResultTrue = new VictoryResult(true);

        Game testGame = createMockedGame();

        // test whether the server.victory() returns false when mocking VictoryResult as false
        when(testGame.getVictoryResult()).thenReturn(testVictoryResultFalse);
        testServer.setGame(testGame);
        assertFalse(testServer.victory());

        // test whether the server.victory() returns true when mocking VictoryResult as true
        when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);
        testServer.setGame(testGame);
        assertTrue(testServer.victory());
    }

    @Test
    public void testVictoryDrawReport() throws IOException {
        Server testServer = new Server("test", 0);
        VictoryResult testVictoryResultTrue = new VictoryResult(true);
        Game testGame = createMockedGame();
        when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);

        testServer.setGame(testGame);
        testServer.victory();
        verify(testGame, times(1)).setVictoryPlayerId(Player.PLAYER_NONE);
        verify(testGame, times(1)).setVictoryTeam(Player.TEAM_NONE);
    }

    @Test
    public void testVictoryFalseReport() throws IOException {
        Server testServer = new Server("test", 0);
        VictoryResult testVictoryResultTrue = new VictoryResult(false);
        Game testGame = createMockedGame();
        when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);

        testServer.setGame(testGame);
        testServer.victory();
        verify(testGame, times(1)).cancelVictory();
    }

    @Test
    public void testCancelVictory() throws IOException {
        Server testServer = new Server("test", 0);
        VictoryResult testVictoryResultTrue = new VictoryResult(false);
        Game testGame = createMockedGame();
        when(testGame.getVictoryResult()).thenReturn(testVictoryResultTrue);
        when(testGame.isForceVictory()).thenReturn(true);

        testServer.setGame(testGame);
        testServer.victory();
        verify(testGame, times(1)).cancelVictory();
    }

    @Test
    public void testVictoryWinReports() throws IOException {
        Server testServer = new Server("test", 0);

        int winner = 1;

        // Mock a win victory result
        // Only 1 report should be generated as the team is set to TEAM_NONE
        Game testGame = createMockedGame();
        VictoryResult victoryResult = mock(VictoryResult.class);
        when(victoryResult.processVictory(testGame)).thenCallRealMethod();
        when(victoryResult.getReports()).thenReturn(new ArrayList<>());
        when(victoryResult.victory()).thenReturn(true);
        when(victoryResult.isDraw()).thenReturn(false);
        when(victoryResult.getWinningPlayer()).thenReturn(winner);
        when(victoryResult.getWinningTeam()).thenReturn(Player.TEAM_NONE);

        Player mockedPlayer = mock(Player.class);
        when(mockedPlayer.getName()).thenReturn("The champion");
        when(mockedPlayer.getColour()).thenReturn(PlayerColour.BLUE);

        when(testGame.getVictoryResult()).thenReturn(victoryResult);
        when(testGame.getPlayer(winner)).thenReturn(mockedPlayer);

        testServer.setGame(testGame);
        testServer.victory();

        assertSame(1, testServer.getvPhaseReport().size());

        // Second test server tests with both a team != TEAM_NONE and a player != PLAYER_NONE
        // Two reports should be generated
        Server testServer2 = new Server("test", 0);

        when(victoryResult.getWinningTeam()).thenReturn(10);
        when(victoryResult.getReports()).thenReturn(new ArrayList<>());
        testServer2.setGame(testGame);
        testServer2.victory();

        assertSame(2, testServer2.getvPhaseReport().size());
    }
}
