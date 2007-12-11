package megamek.server.victory;

/**
 *  quick implementation of a Victory.Report
 */
public class SimpleReport
implements Victory.Report
{
    protected int team;
    protected int player;
    protected boolean victory;
    
    public SimpleReport(
                boolean win,
                int player,
                int team)
    {
        this.victory=win;
        this.player=player;
        this.team=team;
    }
    public boolean victory()
    {
        return victory;
    }
    public int winningPlayerId()
    {
        return player;
    }
    public int winningTeamId()
    {
        return team;
    }
}