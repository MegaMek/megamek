package megamek.common.containers;

import java.io.Serial;
import java.util.Vector;

/**
 * TODO: Adjust and replace this class with one that has the data as a member of the class versus just extending it.
 *
 * @param <T> the datatype for the vector.
 *
 * @author dirk This class is one of the common container types which is a player id combined with a list of other data,
 *       such as when transmitting artillery auto hit coordinates.
 */
public class PlayerIDAndList<T> extends Vector<T> {
    @Serial
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

    /**
     * Clone method as is required for Vector. SuppressWarnings is needed as there is no clean way to clone a Vector of
     * Data with type checking.
     *
     * @return a clone of {@link PlayerIDAndList} that is templated to type T
     */
    @SuppressWarnings("unchecked")
    public PlayerIDAndList<T> clone() {
        PlayerIDAndList<T> playerIDandList = (PlayerIDAndList<T>) super.clone();
        playerIDandList.playerID = playerID;
        return playerIDandList;
    }
}
