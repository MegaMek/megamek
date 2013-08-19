/**
 *
 */
package megamek.server.commands;

import java.util.Enumeration;

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
                "Show the ids of all entities owned by this player. Ussage: /listEntities");
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        IPlayer p = server.getGame().getPlayer(connId);
        if (null == p) {
            return;
        }
        for (Enumeration<Entity> i = server.getGame().getEntities(); i.hasMoreElements(); ) {
            try {
                Entity ent = i.nextElement();
                if (ent.getOwnerId() == connId) {
                    server.sendServerChat(connId, ent.getId() + " - " + ent.getDisplayName());
                }
            } catch (NumberFormatException nfe) {
            } catch (NullPointerException npe) {
            } catch (IndexOutOfBoundsException ioobe) {
            }
        }
    }
}
