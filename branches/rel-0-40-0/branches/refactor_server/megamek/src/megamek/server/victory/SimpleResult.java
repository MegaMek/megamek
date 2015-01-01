package megamek.server.victory;
import java.util.*;
import megamek.common.Report;
import megamek.common.Player;
import java.io.*;

/**
 *  quick implementation of a Victory.Result
 */
public class SimpleResult
extends VictoryResult
implements Victory.Result
{
    public SimpleResult(
                boolean win,
                int player,
                int team)
    {
        super(win);
        if(player!=Player.PLAYER_NONE)
            addPlayerScore(player,1.0);
        if(team!=Player.TEAM_NONE)
            addTeamScore(team,1.0);
    }
}