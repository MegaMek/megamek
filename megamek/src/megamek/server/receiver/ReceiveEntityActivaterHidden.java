package megamek.server.receiver;

import megamek.common.Entity;
import megamek.common.enums.GamePhase;
import megamek.common.net.Packet;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

public class ReceiveEntityActivaterHidden {
    /**
     *
     * @param server
     * @param c the packet to be processed
     * @param connIndex the id for connection that received the packet.
     */
    public static void receiveEntityActivateHidden(Server server, Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        GamePhase phase = (GamePhase) c.getObject(1);
        Entity e = server.getGame().getEntity(entityId);
        if (connIndex != e.getOwnerId()) {
            LogManager.getLogger().error("Player " + connIndex
                    + " tried to activate a hidden unit owned by Player " + e.getOwnerId());
            return;
        }
        e.setHiddenActivationPhase(phase);
        server.entityUpdate(entityId);
    }
}
