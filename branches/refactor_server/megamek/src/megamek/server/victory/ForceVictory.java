package megamek.server.victory;
import megamek.common.IGame;
import megamek.common.Team;
import megamek.common.Player;
import java.util.*;
/**
 *  implementation of player-agreed victory
 */
public class ForceVictory
implements Victory
{

    public ForceVictory()
    {}
    public Victory.Result victory(IGame game)
    {
		if (!game.isForceVictory())
            return new SimpleNoResult();
        int victoryPlayerId = game.getVictoryPlayerId();
        int victoryTeam = game.getVictoryTeam();
        Vector<Player> players = game.getPlayersVector();
        boolean forceVictory = true;

        // Individual victory.
        if (victoryPlayerId != Player.PLAYER_NONE) {
            for (int i = 0; i < players.size(); i++) {
                Player player = players.elementAt(i);

                if (player.getId() != victoryPlayerId
                        && !player.isObserver()) {
                    if (!player.admitsDefeat()) {
                        forceVictory = false;
                        break;
                    }
                }
            }
        }
        // Team victory.
        if (victoryTeam != Player.TEAM_NONE) {
            for (int i = 0; i < players.size(); i++) {
                Player player = players.elementAt(i);

                if (player.getTeam() != victoryTeam && !player.isObserver()) {
                    if (!player.admitsDefeat()) {
                        forceVictory = false;
                        break;
                    }
                }
            }
        }

        if (forceVictory) {
            return new SimpleResult(true,victoryPlayerId,victoryTeam);
        }
        /* modifying state is naaastyy
        for (int i = 0; i < players.size(); i++) {
            Player player = players.elementAt(i);
            player.setAdmitsDefeat(false); 
        }*/

        //cancelVictory(); //modifying state is nasty
        
        return new SimpleNoResult();
    }
}