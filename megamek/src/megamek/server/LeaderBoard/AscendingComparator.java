package megamek.server.LeaderBoard;


import java.util.Comparator;

public  class AscendingComparator implements Comparator<LeaderBoardEntry> {
    @Override
    public int compare(LeaderBoardEntry o1, LeaderBoardEntry o2) {
        return o2.getElo().compareTo(o1.getElo());
    }
}
