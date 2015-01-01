package megamek.server.victory;
import java.util.*;
import megamek.common.IGame;
import megamek.common.Team;
import megamek.common.Player;
/**
 *  abstract baseclass for bv-checking victory implementations
 */
public abstract class AbstractBVVictory
implements Victory
{
    public int getFriendlyBV(IGame game,Player player)
    {
        int ret=0;
        for (Enumeration<Player> f = game.getPlayers(); f
                .hasMoreElements();) {
            Player other = f.nextElement();
            if (other.isObserver())
                continue;
            if (!other.isEnemyOf(player)) {
                ret+=other.getBV();
            }
        }    
        return ret;
    }
    public int getEnemyBV(IGame game,Player player)
    {
        int ret=0;
        for (Enumeration<Player> f = game.getPlayers(); f
                .hasMoreElements();) {
            Player other = f.nextElement();
            if (other.isObserver())
                continue;
            if (other.isEnemyOf(player)) {
                ret+= other.getBV();
            }
        }    
        return ret;
    }
    public int getEnemyInitialBV(IGame game,Player player)
    {
        int ret=0;
        for (Enumeration<Player> f = game.getPlayers(); f
                .hasMoreElements();) {
            Player other = f.nextElement();
            if (other.isObserver())
                continue;
            if (other.isEnemyOf(player)) {
                ret += other.getInitialBV();
            } 
        }    
        return ret;
    }
    
} 