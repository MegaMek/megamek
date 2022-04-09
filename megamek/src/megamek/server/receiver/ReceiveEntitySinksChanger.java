package megamek.server.receiver;

import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.net.Packet;
import megamek.server.Server;

public class ReceiveEntitySinksChanger {
    /**
     * Receive and process an Entity Heat Sinks Change Packet
     * @param server
     * @param c the packet to be processed
     * @param connIndex the id for connection that received the packet.
     */
    public static void receiveEntitySinksChange(Server server, Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        int numSinks = c.getIntValue(1);
        Entity e = server.getGame().getEntity(entityId);
        if ((e instanceof Mech) && (connIndex == e.getOwnerId())) {
            ((Mech) e).setActiveSinksNextRound(numSinks);
        }
    }
}
