package megamek.server.LeaderBoard;

import megamek.common.Player;
import java.util.ArrayList;



public class LeaderBoard {

    private static LeaderBoard SINGLETON;
    private LeaderBoard() {
        leaderboard = new ArrayList<>();
    }

    private final ArrayList<LeaderBoardEntry> leaderboard;


    public void addRanking(Player player, int elo) {
        addRanking(new LeaderBoardEntry(player,elo));
    }
    private void addRanking(LeaderBoardEntry e) {
        leaderboard.add(e);
    }

    private void sortHighscores() {
        this.leaderboard.sort(new AscendingComparator());
    }
    
    public LeaderBoardEntry get(Player player) {
        for (LeaderBoardEntry leaderBoardEntry : leaderboard) {
            if(leaderBoardEntry.equals(player))
                return leaderBoardEntry;
        }
        return null;
    }

    public ArrayList<LeaderBoardEntry> getSortedRankings() {
        sortHighscores();
        return leaderboard;
    }

    public void emptyBoard() {
        leaderboard.clear();
    }

    public static LeaderBoard get() {
        if(SINGLETON == null)
            SINGLETON = new LeaderBoard();
        return SINGLETON;
    }

}
