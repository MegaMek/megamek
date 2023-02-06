package megamek.client;

import megamek.client.commands.ClientCommand;
import megamek.common.IGame;
import megamek.common.InGameObject;
import megamek.common.Player;

import java.util.*;

public interface GameClient {

    //region Server Connection

    /** Attempt to connect to the specified host */
    boolean connect();

    String getName();

    int getPort();

    String getHost();

    void die();

    //endregion

    //region Game Content

    /**
     * Returns the game of this client as a game-type independent IGame. Note that the game
     * object is only updated, not replaced and a reference to it can therefore be kept.
     *
     * @return The game of this client
     */
    IGame getIGame();

    /** @return The in-game object associated with the given ID. */
    default Optional<InGameObject> getInGameObject(int id) {
        return getIGame().getInGameObject(id);
    }

    /** @return A list of all in-game objects of the game. This list is copied and may be safely modified. */
    default List<InGameObject> getInGameObjects() {
        return getIGame().getInGameObjects();
    }

    /** @return The player with the given ID, or null if there is no such player. */
    default Player getPlayer(int id) {
        return getIGame().getPlayer(id);
    }

    /** @return The ID of the player playing at this Client. */
    int getLocalPlayerNumber();

    /**
     * Sets the ID of the player playing at this Client.
     * // TODO : only used by AddBotUtil -> could be included in a bot's constructor and removed here
     * @param localPlayerNumber
     */
    void setLocalPlayerNumber(int localPlayerNumber);

    /** @return The local player (playing at this Client). */
    default Player getLocalPlayer() {
        return getPlayer(getLocalPlayerNumber());
    }

    /** @return True when there is a player (incl. bot) with the given ID in the game. */
    default boolean playerExists(int id) {
        return getPlayer(id) != null;
    }

    //endregion

    //region Bots

    /**
     * Returns the map containing this Client's local bots, wherein the key is the bot's player name and the value
     * the Client.
     *
     * @return This Client's local bots mapped to their player name
     */
    Map<String, Client> getBots();

    /** @return True when the given player is a bot added/controlled by this Client. */
    default boolean isLocalBot(Player player) {
        return getBots().containsKey(player.getName());
    }

    /**
     * Returns the Client associated with the given local bot player. If
     * the player is not a local bot, returns null.
     *
     * @return The Client for the given player if it's a bot, null otherwise
     */
    default Client getBotClient(Player player) {
        return getBots().get(player.getName());
    }

    //endregion

    /** Sends a "this player is done/not done" message to the server. */
    void sendDone(boolean done);

    //region ClientCommands

    /** @return The client chat command associated with the given name. */
    ClientCommand getCommand(String name);

    /** @return All registered client chat command associated with the given name. */
    Set<String> getAllCommandNames();

    //endregion

}
