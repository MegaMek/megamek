package megamek.server.victory;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MechWarrior;
import megamek.common.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.Map;

import java.util.Vector;


@RunWith(value = JUnit4.class)
public class KillCountVictoryTest {

    private int killcount = 100;
    @Test
    public void testWhenVictoryIsNotEarnYetByKillCount() {

        KillCountVictory killCountVictory = new KillCountVictory(killcount);
        Game testGame = Mockito.mock(Game.class);
        Mockito.when(testGame.getWreckedEntities()).thenReturn((new Vector<Entity>()).elements());
        Mockito.when(testGame.getCarcassEntities()).thenReturn((new Vector<Entity>()).elements());
        Mockito.when(testGame.getCarcassEntities()).thenReturn((new Vector<Entity>()).elements());
        Mockito.when(testGame.getEntityFromAllSources(Mockito.anyInt())).thenReturn(null);

        Player winner = Mockito.mock(Player.class);
        int winnerId = 1;

        Player looser = Mockito.mock(Player.class);
        int looserID = 2;

        Mockito.when(winner.getId()).thenReturn(winnerId);
        Mockito.when(looser.getId()).thenReturn(looserID);
        Mockito.when(testGame.getPlayer(winner.getId())).thenReturn(winner);


        Map<String, Object> ctx = Mockito.mock(Map.class);
        killCountVictory.victory(testGame,ctx);

        Mockito.verify(testGame, Mockito.times(1)).getWreckedEntities();
        Mockito.verify(testGame, Mockito.times(1)).getCarcassEntities();
        //Mockito.verify(testGame, Mockito.times(1)).getEntityFromAllSources(Mockito.anyInt());

    }
}
