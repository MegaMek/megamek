package megamek.server.victory;
import megamek.common.Player;

/**
 *  quick implementation of a Victory.Result
 *  to say "nobody won yet"
 */
public class SimpleNoResult
extends SimpleResult
{    
    public SimpleNoResult()
    {
        super(false,Player.PLAYER_NONE,Player.TEAM_NONE);
    }
}