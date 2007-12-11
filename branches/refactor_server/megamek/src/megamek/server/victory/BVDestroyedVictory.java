package megamek.server.victory;
import java.util.*;
import megamek.common.IGame;
import megamek.common.Team;
import megamek.common.Player;
import megamek.common.Report;

/**
 *  implementation which will match when a certain percentage of all 
 *  enemy BV is destroyed. 
 *
 *  NOTe: this could be improved by giving more points for killing 
 *         more than required amount 
 */
public class BVDestroyedVictory
extends AbstractBVVictory
{
    protected int destroyedPercent;
    public BVDestroyedVictory(int destroyedPercent)
    {
        this.destroyedPercent=destroyedPercent;
    }
    
    public Victory.Result victory(
                        IGame game)
    {
        boolean victory=false;
        VictoryResult vr=new VictoryResult(true);
		// now check for detailed victory conditions...
        HashSet<Integer> doneTeams = new HashSet<Integer>();
        for (Enumeration<Player> e = game.getPlayers(); e.hasMoreElements();) {
            Player player = e.nextElement();
            if (player.isObserver())
                continue;
            int fbv = 0;
            int ebv = 0;
            int eibv = 0;
            int team = player.getTeam();
            if (team != Player.TEAM_NONE) {
                if (doneTeams.contains(team))
                    continue; // skip if already
                doneTeams.add(team);
            }
            fbv=getFriendlyBV(game,player);
            ebv=getEnemyBV(game,player);
            eibv=getEnemyInitialBV(game,player);
            
            if ((ebv * 100) / eibv <= 100 - destroyedPercent) {
                Report r = new Report(7105, Report.PUBLIC);
                victory=true;
                if (team == Player.TEAM_NONE) {
                    r.add(player.getName());
                    vr.addPlayerScore(player.getId(),1.0);
                } else {
                    r.add("Team " + team);
                    vr.addTeamScore(team,1.0);
                }
                r.add(100 - ((ebv * 100) / eibv));
                vr.addReport(r);
            }            
        }//end for
        if(victory)
            return vr;
        return new SimpleNoResult();
        
    }
}