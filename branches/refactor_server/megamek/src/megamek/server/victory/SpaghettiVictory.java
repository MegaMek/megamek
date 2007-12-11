package megamek.server.victory;
import java.util.*;
import megamek.common.IGame;
import megamek.common.Team;
import megamek.common.Player;
/**
 *  This is the original implementation of victory moved under the 
 *  new infrastructure
 */
public class SpaghettiVictory
implements Victory
{
    protected Victory force=new ForceVictory();
    protected Victory lastMan=new LastManStandingVictory();
    protected Victory check=new CheckVictory(new NoodleVictory());
    
    public Victory.Result victory(
                        IGame game)
    {
        Victory.Result ret=null;
        ret=force.victory(game);
        if(ret.victory())
            return ret;
            
        ret=lastMan.victory(game);
        if(ret.victory())
            return ret;
            
        return check.victory(game);
    }
}