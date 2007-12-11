package megamek.server.victory;
import java.util.*;
import megamek.common.IGame;
import megamek.common.Team;
import megamek.common.Player;
import megamek.common.Report;
/**
 *  a summing victory condition 
 *  will first sum all given victory conditions, if any of them are 
 *  in victory state AND someone is over the threshold, then will 
 *  return victory , otherwise just the scores
 *
 *  the scores will be an average of the given victory scores. 
 */
public class SummingThresholdVictory
implements Victory
{
    protected Victory[] vs;
    protected int thr;
    public SummingThresholdVictory(
                    int threshold,
                    Victory[] victories)
    {
        this.vs=victories;
        this.thr=threshold;
    }
    public Victory.Result victory(
                        IGame game)
    {
        boolean victory=false;
        VictoryResult vr=new VictoryResult(true);
        
        // combine scores
        for(Victory v:vs)
        {
            Victory.Result res=v.victory(game);
            for(Report r:res.getReports())
            {
                vr.addReport(r);
            }
            if(res.victory())
                victory=true;
            for(int pl:res.getPlayers())
            {
                vr.addPlayerScore(
                        pl,
                        vr.getPlayerScore(pl)+res.getPlayerScore(pl));
            }
            for(int t:res.getTeams())
            {
                vr.addTeamScore(
                        t,
                        vr.getTeamScore(t)+res.getTeamScore(t));
            }
        }
        //find highscore for thresholding, also divide the score 
        //to an average
        double highScore=0.0;
        for(int pl:vr.getPlayers())
        {
            double sc=vr.getPlayerScore(pl);
            vr.addPlayerScore(pl,sc/vs.length);
            if(sc>highScore)
                highScore=sc;
        }
        for(int pl:vr.getTeams())
        {
            double sc=vr.getTeamScore(pl);
            vr.addTeamScore(pl,sc/vs.length);
            if(sc>highScore)
                highScore=sc;
        }
        if(highScore<thr)
            victory=false;
        
        vr.setVictory(victory);        
        
        return vr;
    }
}