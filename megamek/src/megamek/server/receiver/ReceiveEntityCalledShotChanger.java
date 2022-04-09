package megamek.server.receiver;

import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.net.Packet;
import megamek.server.Server;

public class ReceiveEntityCalledShotChanger {
    /**
     * receive and process an entity mode change packet
     *
     * @param server
     * @param c the packet to be processed
     * @param connIndex the id for connection that received the packet.
     */
    public static void receiveEntityCalledShotChange(Server server, Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        int equipId = c.getIntValue(1);
        Entity e = server.getGame().getEntity(entityId);
        if (server.getPlayer(connIndex) != e.getOwner()) {
            return;
        }
        Mounted m = e.getEquipment(equipId);

        if (m == null) {
            return;
        }
        m.getCalledShot().switchCalledShot();
    }
}
