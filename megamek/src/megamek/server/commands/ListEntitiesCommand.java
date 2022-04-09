package megamek.server.commands;

import megamek.common.Entity;
import megamek.common.Player;
import megamek.server.Server;

/**
 * @author Jay Lawson (Taharqa)
 */
public class ListEntitiesCommand extends ServerCommand {

    public ListEntitiesCommand(Server server) {
        super(
                server,
                "listEntities",
                "Show the ids of all entities owned by this player. " +
                "Usage: /listEntities");
    }

    /**
     * Run this command with the arguments supplied
     * 
     * @see ServerCommand#run(int, String[])
     */
    @Override
    public void run(int connId, String[] args) {
        Player p = server.getGame().getPlayer(connId);
        if (null == p) {
            return;
        }
        for (Entity ent : server.getGame().getEntitiesVector()) {
            try {
                if (ent.getOwnerId() == connId) {
                    server.sendServerChat(connId,
                            ent.getId() + " - " + ent.getDisplayName());
                }
            } catch (NumberFormatException | NullPointerException | IndexOutOfBoundsException ignored) {
            }
        }
    }
}
