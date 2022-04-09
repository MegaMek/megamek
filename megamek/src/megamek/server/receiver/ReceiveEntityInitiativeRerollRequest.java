package megamek.server.receiver;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

public class ReceiveEntityInitiativeRerollRequest {
    public static void receiveInitiativeRerollRequest(Server server, int connIndex) {
        Player player = server.getPlayer(connIndex);
        if (GamePhase.INITIATIVE_REPORT != server.getGame().getPhase()) {
            StringBuilder message = new StringBuilder();
            if (null == player) {
                message.append("Player #").append(connIndex);
            } else {
                message.append(player.getName());
            }
            message.append(" is not allowed to ask for a reroll at this time.");
            LogManager.getLogger().error(message.toString());
            server.sendServerChat(message.toString());
            return;
        }
        if (server.getGame().hasTacticalGenius(player)) {
            server.getGame().addInitiativeRerollRequest(server.getGame().getTeamForPlayer(player));
        }
        if (null != player) {
            player.setDone(true);
        }
        server.checkReady();
    }
}
