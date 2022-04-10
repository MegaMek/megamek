package megamek.server.victory;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(value = JUnit4.class)
public class EnemyCommanderDestroyedVictoryTest {

    @Test
    public void EnemyCommanderDestroyedVictoryTest(){

        //arrange
        var EnemyCommanderDestroyedVictory = new EnemyCmdrDestroyedVictory();

        var game = GameMockUtils.createMockedGameWithCommanderVictory();

        //act
        var victoryResult=   EnemyCommanderDestroyedVictory.victory(game,null);
        //assert
        assertTrue(victoryResult.victory);

    }
}
