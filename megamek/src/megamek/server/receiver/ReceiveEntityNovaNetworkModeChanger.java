package megamek.server.receiver;

import megamek.common.Entity;
import megamek.common.net.Packet;
import megamek.server.Server;

import java.util.Objects;

public class ReceiveEntityNovaNetworkModeChanger {
    /**
     * receive and process an entity nova network mode change packet
     *
     * @param server
     * @param c the packet to be processed
     * @param connIndex the id for connection that received the packet.
     */
    public static void receiveEntityNovaNetworkModeChange(Server server, Packet c, int connIndex) {

        try {
            int entityId = c.getIntValue(0);
            String networkID = c.getObject(1).toString();
            Entity e = server.getGame().getEntity(entityId);
            if (!Objects.equals(e.getOwner(), server.getPlayer(connIndex))) {
                return;
            }
            // FIXME: Greg: This can result in setting the network to link to
            // hostile units.
            // However, it should be caught by both the isMemberOfNetwork test
            // from the c3 module as well as
            // by the clients possible input.
            e.setNewRoundNovaNetworkString(networkID);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
