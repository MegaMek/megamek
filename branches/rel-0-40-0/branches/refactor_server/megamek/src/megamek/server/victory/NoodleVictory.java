package megamek.server.victory;
import megamek.common.IGame;
import megamek.common.Team;
import megamek.common.Player;
import megamek.common.Report;

import java.util.*;
/**
 *  detailed spaghetti from the old victory code
 *  this assumes game options do not change during game 
 *  (atleast regarding victory conditions) 
 */
public class NoodleVictory
implements Victory
{
    protected Victory v;
    public  NoodleVictory()
    {
    }
    protected void construct(IGame game)
    {
        ArrayList<Victory> victories=new ArrayList<Victory>();
        
        
		// BV related victory conditions
        if (game.getOptions().booleanOption("use_bv_destroyed")) 
        {            
            victories.add(new BVDestroyedVictory(game.getOptions().intOption("bv_destroyed_percent")));
        }
        if (game.getOptions().booleanOption("use_bv_ratio")) 
        {
            victories.add(new BVRatioVictory(game.getOptions().intOption("bv_ratio_percent")));
        }
        
		// Commander killed victory condition
		if (game.getOptions().booleanOption("commander_killed")) 
        {
            victories.add(new LastCmdrStandingVictory());
		}
        // use a summing victory target to check if someone is winning 
        v=new SummingThresholdVictory(
                game.getOptions().intOption("achieve_conditions"),
                victories.toArray(new Victory[0]));        
    }
    public Victory.Result victory(
                            IGame game,
                            HashMap<String,Object> ctx)
    {
        if(v==null)
            construct(game);
        Victory.Result res=v.victory(game,ctx);        
        
        if(res.victory())
        {
            return res;
        }

        if(!res.victory() && game.gameTimerIsExpired())
        {
            return new SimpleDrawResult();
        }


		return res; 
    }
}