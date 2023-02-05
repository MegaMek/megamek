package megamek.client;

import megamek.client.commands.ClientCommand;
import megamek.common.IGame;
import megamek.common.InGameObject;
import megamek.common.Player;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    IGame getIGame();

    default Optional<InGameObject> getInGameObject(int id) {
        return getIGame().getInGameObject(id);
    }

    /**
     * Returns an enumeration of the entities in game.entities
     */
    default List<InGameObject> getInGameObjects() {
        return getIGame().getInGameObjects();
    }

    int getLocalPlayerNumber() ;

    void setLocalPlayerNumber(int localPlayerNumber);

    /** @return The player with the given ID, or null if there is no such player. */
    default Player getPlayer(int id) {
        return getIGame().getPlayer(id);
    }

    /** @return The local player (playing at this Client). */
    default Player getLocalPlayer() {
        return getPlayer(getLocalPlayerNumber());
    }

    /** @return True when there is a player (incl. bot) with the given ID in this Client's game. */
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

    void sendDone(boolean done);

    void sendNextPlayer(); // ??????

    //region ClientCommands

    ClientCommand getCommand(String name);

    Enumeration<String> getAllCommandNames();

    void registerCommand(ClientCommand command);

    //endregion

}
