package megamek.server.victory;
import megamek.common.IGame;


/**
 *  interface for classes judging whether a victory occurred or not. 
 *  these classes may not modify game state. Reporting must be done
 *  via the given interface. 
 */
public interface Victory
{
    /**
     *  @return a results with true if victory occured, false if not
     *          may also return null if no victory occurred
     */
    public VictoryResult victory(
                    ReportListener reports,
                    IGame game);
    /**
     *  result class which can be queried for winner and if 
     *  victory occurred
     *  victory might be true and winner "None" if the players
     *  have drawn
     */
    public static interface Result
    {
        /**
         *  @return true if the game is about to end since someone has
         *          completed the victory conditions
         */
        public boolean victory();
        /**
         *  Player id for the winner or Player.PLAYER_NONE
         */
        public int winningPlayerId();
        /**
         *  Team id for the winning team or Player.TEAM_NONE
         */
        public int winningTeamId();
    }    
    /**
     *  implementations call this to report stuff to be added to 
     *  victory reports. This should be the same interface as
     *  Server.addReport
     */
    public static interface ReportListener
    {
        /**
         *  add a report to the reporting queue. this might be
         *  filtered by other layers. 
         */
        public void addReport(Report r);
    }
}