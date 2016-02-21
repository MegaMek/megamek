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

public class TraitorCommand extends ServerCommand {

    public TraitorCommand(Server server) {
        super(
                server,
                "traitor",
                "Switches a player's entity to another player during the end phase. Ussage: /traitor #  # where the first number is the entity id and the second is the new player id");
    }

    /**
     * Run this command with the arguments supplied
     * 
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        try {
            int eid = Integer.parseInt(args[1]);
            Entity ent = server.getGame().getEntity(eid);
            int pid = Integer.parseInt(args[2]);
            IPlayer player = server.getGame().getPlayer(pid);
            if(null == ent) {
                server.sendServerChat(connId, "No such entity");
            }
            else if(ent.getOwner().getId() != connId) {
                server.sendServerChat(connId, "You must own an entity to make it switch sides.");
            }
            else if(null == player) {
                server.sendServerChat(connId, "No such player");
            }
            else if(player.getTeam() == IPlayer.TEAM_UNASSIGNED) {
                server.sendServerChat(connId, "Player must be assigned a team!");
            }
            else if(pid == connId) {
                server.sendServerChat(connId, "You can't switch to the same side!");
            }
            else {
                server.sendServerChat(connId, ent.getDisplayName() + " will switch to " + player.getName() + "'s side at the end of this turn.");
                ent.setTraitorId(pid);
            }
            
        } catch (NumberFormatException nfe) {
        } catch (NullPointerException npe) {
        } catch (IndexOutOfBoundsException ioobe) {
        }
    }

}
