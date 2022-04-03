
package megamek.server.leaderBoardUtil;
import megamek.common.Player;
import megamek.server.LeaderBoard.LeaderBoard;
public interface EloFormula  {

     public int calcElo(LeaderBoard lb, Player winner,Player loser);
}
