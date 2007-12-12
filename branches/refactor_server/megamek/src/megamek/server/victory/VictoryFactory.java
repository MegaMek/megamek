package megamek.server.victory;

/**
 *  interface for VictoryFactories, ie. classes which construct 
 *  Victory objects or Victory object hierarchies based on a 
 *  given string. The string might be just a list of instructions, 
 *  gibberish , or an url where to retrieve the real information or 
 *  a combination of these. 
 *
 *  Implementors must implement a publicly accessible default constructor
 *  and must not store state outside of methods. 
 *
 *  Also in general a Victory generated with this factory should not 
 *  alter its workings based on settings fetched during the game from
 *  some source. like game options=) bad form=) Those options should
 *  be given in the victory-string as a parameter. 
 */
public interface VictoryFactory
{
    /**
     *  @param conditions - depending on the implementation describes
     *          either the conditions for Victory or a place where to 
     *          get them. 
     */
    public Victory createVictory(String victory);
}