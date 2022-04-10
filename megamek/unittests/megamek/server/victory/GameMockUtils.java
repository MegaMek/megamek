package megamek.server.victory;

import megamek.common.Game;
import megamek.common.Player;
import megamek.common.force.Forces;
import megamek.common.options.GameOptions;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Vector;

public class GameMockUtils {
    public static Game createMockedGame() {
        Game testGame = Mockito.mock(Game.class);
        Forces testForces = new Forces(testGame);

        Player winner = Mockito.mock(Player.class);
        Player looser = Mockito.mock(Player.class);

        var players = new Vector<Player>(2);
        players.add(winner);
        players.add(looser);
        Mockito.when(testGame.getGameListeners()).thenReturn(new Vector<>());
        Mockito.when(testGame.getEntities()).thenReturn(Collections.emptyIterator());
        Mockito.when(testGame.getPlayers()).thenReturn(Collections.emptyEnumeration());
        Mockito.when(testGame.getPlayersVector()).thenReturn(players);
        Mockito.when(testGame.getAttacks()).thenReturn(Collections.emptyEnumeration());
        Mockito.when(testGame.getForces()).thenReturn(testForces);
        Mockito.when(testGame.getOptions()).thenReturn(new GameOptions());
        Mockito.when(testGame.getLiveCommandersOwnedBy(winner)).thenReturn(1);
        Mockito.when(testGame.getLiveCommandersOwnedBy(winner)).thenReturn(0);

        return testGame;
    }
}
