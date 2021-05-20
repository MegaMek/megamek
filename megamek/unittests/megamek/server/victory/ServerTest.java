package megamek.server.victory;

import megamek.common.event.GameListener;
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
    @Test
    public void testVictory() throws IOException {
        Server testServer = new Server("test", 123);
        Victory testVictory = Mockito.mock(Victory.class);
        VictoryResult testVictoryResult = new VictoryResult(false);

        IGame testGame = Mockito.mock(IGame.class);
        Mockito.when(testGame.getVictory()).thenReturn(testVictory);
        Mockito.when(testGame.getGameListeners()).thenReturn(new Vector<GameListener>());
        Mockito.when(testGame.getEntities()).thenReturn(Collections.emptyIterator());
        Mockito.when(testGame.getPlayers()).thenReturn(Collections.emptyEnumeration());
        Mockito.when(testGame.getAttacks()).thenReturn(Collections.emptyEnumeration());
        Mockito.when(testVictory.checkForVictory(testGame, testGame.getVictoryContext())).thenReturn(testVictoryResult);
        testServer.setGame(testGame);

        TestCase.assertFalse(testServer.victory());
    }
}


