/**
 * 
 */
package megamek.server.commands;

import megamek.common.Entity;
import megamek.common.IPlayer;
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
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        IPlayer p = server.getGame().getPlayer(connId);
        if(null == p) {
            return;
        }
        for (Entity ent : server.getGame().getEntitiesVector()) {
            try {
                if(ent.getOwnerId() == connId) {
                    server.sendServerChat(connId,
                            ent.getId() + " - " + ent.getDisplayName());
                }
            } catch (NumberFormatException nfe) {
            } catch (NullPointerException npe) {
            } catch (IndexOutOfBoundsException ioobe) {
            }
        }
    }
}
