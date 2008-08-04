package megamek.common.containers;

import java.util.Vector;

/**
 * @author dirk This class is one of the common container types which is a
 *         player id combined with a list of other data, such as when
 *         transmitting artillery auto hit coordinates.
 * @param <Data> the datatype for the vector.
 */
public class PlayerIDandList<Data> extends Vector<Data> {
    private static final long serialVersionUID = 391550235984284684L;
    private int playerID;

    /**
     * Returns the player ID
     * 
     * @return the playerID
     */
    public int getPlayerID() {
        return playerID;
    }

    /**
     * sets the playerID
     * 
     * @param playerID the playerID
     */
    public void setPlayerID(int playerID) {
        this.playerID = playerID;
    }
}
