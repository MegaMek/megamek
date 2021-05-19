package megamek.server.victory;

import org.junit.Assert;
import junit.framework.TestCase;
import megamek.common.IGame;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.mockito.Mockito.doReturn;


@RunWith(JUnit4.class)
public class VictoryTest {
    @Test
    public void testVictory() {
        Victory testVictory = Mockito.mock(Victory.class);
        VictoryResult testVictoryResult = Mockito.mock(VictoryResult.class);
        IGame testGame = Mockito.mock(IGame.class);
        Mockito.when(testGame.getVictory()).thenReturn(testVictory);
        Mockito.when(testVictory.checkForVictory(testGame, testGame.getVictoryContext())).thenReturn(testVictoryResult);

    }
}


