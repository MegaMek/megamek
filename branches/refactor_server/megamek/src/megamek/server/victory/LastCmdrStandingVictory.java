package megamek.server.victory;
import megamek.common.IGame;
import megamek.common.Team;
import megamek.common.Player;
import java.util.*;
/**
 *  implements "last commander standing" 
 */
public class LastCmdrStandingVictory
implements Victory
{
    public  LastCmdrStandingVictory()
    {
    }
    public Victory.Result victory(IGame game)
    {
        // check all players/teams for aliveness
        int playersAlive = 0;
        Player lastPlayer = null;
        boolean oneTeamAlive = false;
        int lastTeam = Player.TEAM_NONE;
        boolean unteamedAlive = false;
        for (Enumeration<Player> e = game.getPlayers(); e.hasMoreElements();) {
            Player player = e.nextElement();
            int team = player.getTeam();
            if (game.getLiveCommandersOwnedBy(player) <= 0) {
                continue;
            }
            // we found a live one!
            playersAlive++;
            lastPlayer = player;
            // check team
            if (team == Player.TEAM_NONE) {
                unteamedAlive = true;
            } else if (lastTeam == Player.TEAM_NONE) {
                // possibly only one team alive
                oneTeamAlive = true;
                lastTeam = team;
            } else if (team != lastTeam) {
                // more than one team alive
                oneTeamAlive = false;
                lastTeam = team;
            }
        }

        // check if there's one player alive
        if (playersAlive < 1) 
        {
            return new SimpleDrawResult();            
        } else if (playersAlive == 1) {
            if (lastPlayer != null
                    && lastPlayer.getTeam() == Player.TEAM_NONE) 
            {
                // individual victory
                return new SimpleResult(true,lastPlayer.getId(),lastPlayer.getTeam());
            }
        }

        // did we only find one live team?
        if (oneTeamAlive && !unteamedAlive) {
            return new SimpleResult(true,Player.PLAYER_NONE,lastTeam);
        }
        return new SimpleNoResult();
    }
}