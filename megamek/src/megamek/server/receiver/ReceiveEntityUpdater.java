package megamek.server.receiver;

import megamek.common.Entity;
import megamek.common.enums.GamePhase;
import megamek.common.net.Packet;
import megamek.server.Server;
import megamek.server.ServerLobbyHelper;

public class ReceiveEntityUpdater {
    /**
     * Updates an entity with the info from the client. Only valid to do this
     * during the lounge phase, except for heat sink changing.
     * @param server
     * @param c the packet to be processed
     * @param connIndex the id for connection that received the packet.
     */
    public static void receiveEntityUpdate(Server server, Packet c, int connIndex) {
        Entity entity = (Entity) c.getObject(0);
        Entity oldEntity = server.getGame().getEntity(entity.getId());
        if ((oldEntity != null) && (!oldEntity.getOwner().isEnemyOf(server.getPlayer(connIndex)))) {
            server.getGame().setEntity(entity.getId(), entity);
            server.entityUpdate(entity.getId());
            // In the chat lounge, notify players of customizing of unit
            if (server.getGame().getPhase() == GamePhase.LOUNGE) {
                server.sendServerChat(ServerLobbyHelper.entityUpdateMessage(entity, server.getGame()));
            }
        }
    }
}
