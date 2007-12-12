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
    protected double hiScore=0;
    public VictoryResult(
                boolean win)
    {
        this.victory=win;
        tr=new Throwable();
    }
    
    
    //TBD add getWinningPlayer and getWinningTeam
    // which should return NONE:s to signal draw if too complex winnage
    
    protected void updateHiScore()
    {
        //used to calculate winner
        hiScore=Double.MIN_VALUE;
        for(Double d:playerScore.values())
        {
            if(d>hiScore)
                hiScore=d;
        }
        for(Double d:teamScore.values())
        {
            if(d>hiScore)
                hiScore=d;        
        }
        
    }
    public void addPlayerScore(int id,double score)
    {
        playerScore.put(id,score);
        updateHiScore();
    }
    public void addTeamScore(int id,double score)
    {
        teamScore.put(id,score);
        updateHiScore();
    }
    public boolean isWinningPlayer(int id)
    {
        double d=getPlayerScore(id);
        //two decimal compare..
        return ((d*100)%100)==((hiScore*100)%100);
    }
    public boolean isWinningTeam(int id)
    {
        double d=getTeamScore(id);
        //two decimal compare..
        return ((d*100)%100)==((hiScore*100)%100);
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