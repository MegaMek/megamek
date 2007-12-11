package megamek.server.victory;
import java.util.*;
import megamek.common.IGame;
import megamek.common.Team;
import megamek.common.Player;
import megamek.common.Report;

/**
 *  implements bv-ratio victory checking
 *  ratio is defined as friendlybv/enemybv>(bvratiopercent/100)=>win
 *  so this comparison is valid for 3 team combat , but you must drop
 *  ALL enemies combined to below given ratio. 
 *
 *  if multiple players reach this goal at the same time, the result is 
 *  declared a draw
 *  NOTENOTE: this could be improved to take into account ratios which 
 *              exceed given ratio
 */
public class BVRatioVictory
extends AbstractBVVictory
{
    protected int ratio;
    public BVRatioVictory(int ratio)
    {
        this.ratio=ratio;
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
            int team = player.getTeam();
            if (team != Player.TEAM_NONE) {
                if (doneTeams.contains(team))
                    continue; // skip if already
                doneTeams.add(team);
            }
            fbv=getFriendlyBV(game,player);
            ebv=getEnemyBV(game,player);
            //eibv=getEnemyInitialBV(game,player);


            if (ebv == 0
                    || (100 * fbv) / ebv >= ratio) {
                Report r = new Report(7100, Report.PUBLIC);
                victory=true;
                if (team == Player.TEAM_NONE) {
                    r.add(player.getName());
                    vr.addPlayerScore(player.getId(),1.0);
                } else {
                    r.add("Team " + team);
                    vr.addTeamScore(team,1.0);
                }
                r.add(ebv == 0 ? 9999 : (100 * fbv) / ebv);
                vr.addReport(r);
            }
        }//end for
        if(victory)
            return vr;
        return new SimpleNoResult();
    }
}