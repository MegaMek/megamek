package megamek.server.victory;
import java.util.*;
import megamek.common.Report;
import java.io.*;
/**
 *  quick implementation of a Victory.Result
 *  stores player scores and a flag if game-ending victory is achieved or 
 *  not
 */
public class VictoryResult
implements Victory.Result
{
    protected boolean victory;
    protected Throwable tr;
    protected ArrayList<Report> reports=new ArrayList<Report>();
    protected HashMap<Integer,Double> playerScore=
                        new HashMap<Integer,Double>();
    protected HashMap<Integer,Double> teamScore=
                        new HashMap<Integer,Double>();
                    
    public VictoryResult(
                boolean win)
    {
        this.victory=win;
        tr=new Throwable();
    }
    
    
    //TBD add getWinningPlayer and getWinningTeam
    // which should return NONE:s to signal draw if too complex winnage
    
    
    public void addPlayerScore(int id,double score)
    {
        playerScore.put(id,score);
    }
    public void addTeamScore(int id,double score)
    {
        teamScore.put(id,score);
    }
    public boolean victory()
    {
        return victory;
    }        
    public void setVictory(boolean b)
    {
        this.victory=b;
    }
    public double getPlayerScore(int id)
    {
        if(playerScore.get(id)==null)
            return 0.0;
        else return playerScore.get(id);
    }
    
    public int[] getPlayers()
    {
        return intify(playerScore.keySet().toArray(new Integer[0]));
    }
    public double getTeamScore(int id)
    {
        if(teamScore.get(id)==null)
            return 0.0;
        else
            return teamScore.get(id);
    }
    public int[] getTeams()
    {
        return intify(teamScore.keySet().toArray(new Integer[0]));
    }
    
    public void addReport(Report r)
    {
        reports.add(r);
    }
    public ArrayList<Report> getReports()
    {
        return reports;
    }
    protected String getTrace()
    {
        StringWriter sw=new StringWriter();
        PrintWriter pr=new PrintWriter(sw);
        tr.printStackTrace(pr);
        pr.flush();
        return sw.toString();
    }
    private int[] intify(Integer[] ar)
    {
        int[] ret=new int[ar.length];
        for(int i=0;i<ar.length;i++)
            ret[i]=ar[i];
        return ret;
    }
    public String toString()
    {
        return "victory provided to you by:"+getTrace();
    }
}