package megamek.server.commands;

import megamek.common.Entity;
import megamek.common.Player;
import megamek.server.gameManager.GameManager;
import megamek.server.Server;

/**
 * @author Jay Lawson (Taharqa)
 */
public class ListEntitiesCommand extends ServerCommand {

    private final GameManager gameManager;

    public ListEntitiesCommand(Server server, GameManager gameManager) {
        super(server, "listEntities",
                "Show the ids of all entities owned by this player. Usage: /listEntities");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        Player p = server.getGame().getPlayer(connId);
        if (null == p) {
            return;
        }

        for (Entity ent : gameManager.getGame().getEntitiesVector()) {
            try {
                if (ent.getOwnerId() == connId) {
                    server.sendServerChat(connId, ent.getId() + " - " + ent.getDisplayName());
                }
            } catch (Exception ignored) {

            }
        }
    }
}
