package megamek.common;

import java.io.Serializable;

/**
 * This interface is implemented by player turns (e.g. GameTurn), where a specific player has to declare
 * their action(s).
 */
public interface PlayerTurn extends Serializable  {

    /**
     * @return this player turn's player ID
     */
    int getPlayerId();

    /**
     * @return true if the player is valid.
     */
    default boolean isValid(int playerId, IGame game) {
        return playerId == getPlayerId();
    }


    /**
     * Returns true if this player turn requires the player to provide actions for more than one unit. This is
     * true for example when vehicles must be moved as a lance. This is not equivalent to providing multiple
     * actions, such as in firing multiple weapons. That is not considered a multi-turn.
     *
     * @return True when this turn requires actions for at least two units
     */
    boolean isMultiTurn();
}
