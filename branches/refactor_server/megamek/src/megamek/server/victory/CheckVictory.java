package megamek.server.victory;
import megamek.common.IGame;
import megamek.common.Team;
import megamek.common.Player;
import java.util.*;
/**
 *  implementation of a filter which will wait until the 
 *  game.gameTimerIsExpired() is true or option "check_victory" is set
 *  before returning whatever the given victory returns. 
 *  otherwise returns SimpleNoResult
 */
public class CheckVictory
implements Victory
{
    protected Victory v;
    public  CheckVictory(Victory v)
    {
        this.v=v;assert(v!=null);
    }
    public Victory.Result victory(
                            IGame game,
                            HashMap<String,Object> ctx)
    {
        //lets call this now to make sure it gets to update its state
        Victory.Result ret=v.victory(game,ctx);
        
		if (!game.gameTimerIsExpired()
				&& !game.getOptions().booleanOption("check_victory")) {
			return new SimpleNoResult();
		}
        return ret;
    }
}