
package megamek.server.leaderBoardUtil;

import megamek.common.Player;
import megamek.server.LeaderBoard.LeaderBoard;
import megamek.server.LeaderBoard.LeaderBoardEntry;

public class EloFormulaDefault implements EloFormula {

        private final int BASE_POINT = 20;
        public int calcElo(LeaderBoard lb, Player winner, Player loser){

            LeaderBoardEntry winnerEntry = lb.get(winner);
            LeaderBoardEntry loserEntry = lb.get(loser);

            double eloFactor = winnerEntry.getElo() / loserEntry.getElo();
            double elo = ( BASE_POINT * eloFactor ) ;
            return (int)elo;
        }
}
