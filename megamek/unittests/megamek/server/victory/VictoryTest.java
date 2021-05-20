package megamek.server.victory;

import megamek.common.Entity;
import megamek.common.IPlayer;
import megamek.common.event.GameListener;
import megamek.server.Server;
import org.junit.Assert;
import junit.framework.TestCase;
import megamek.common.IGame;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.doReturn;


@RunWith(JUnit4.class)
public class VictoryTest {
    @Test
    public void testVictory() throws IOException {
        Server testServer = new Server("test", 123);
        Victory testVictory = Mockito.mock(Victory.class);
        VictoryResult testVictoryResultFalse = new VictoryResult(false);
        VictoryResult testVictoryResultTrue = new VictoryResult(true);


        IGame testGame = Mockito.mock(IGame.class);
        Mockito.when(testGame.getVictory()).thenReturn(testVictory);
        Mockito.when(testGame.getGameListeners()).thenReturn(new Vector<GameListener>());
        Mockito.when(testGame.getEntities()).thenReturn(Collections.emptyIterator());
        Mockito.when(testGame.getPlayers()).thenReturn(Collections.emptyEnumeration());
        Mockito.when(testGame.getAttacks()).thenReturn(Collections.emptyEnumeration());

        //test whether the server.victory() returns false when mocking VictoryResult as false
        Mockito.when(testVictory.checkForVictory(testGame, testGame.getVictoryContext())).thenReturn(testVictoryResultFalse);
        testServer.setGame(testGame);
        TestCase.assertFalse(testServer.victory());

        //test whether the server.victory() returns true when mocking VictoryResult as true
        Mockito.when(testVictory.checkForVictory(testGame, testGame.getVictoryContext())).thenReturn(testVictoryResultTrue);
        testServer.setGame(testGame);
        TestCase.assertTrue(testServer.victory());

    }
}


