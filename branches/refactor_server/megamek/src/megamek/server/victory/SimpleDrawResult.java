package megamek.server.victory;
import megamek.common.Player;

/**
 *  quick implementation of a Victory.Result
 *  to say "nobody won ..its a draw"
 */
public class SimpleDrawResult
extends SimpleResult
{    
    public SimpleDrawResult()
    {
        super(true,Player.PLAYER_NONE,Player.TEAM_NONE);
    }
}