package megamek.server.victory;

/**
 *  
 */
public class SpaghettiVictoryFactory
implements VictoryFactory
{
    /**
     *  This is a really nasty implementation 
     */
    public Victory createVictory(String victory)
    {
        return new SpaghettiVictory();
    }
}