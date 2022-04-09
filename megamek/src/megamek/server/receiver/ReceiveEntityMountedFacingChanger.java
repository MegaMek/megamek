package megamek.server.receiver;

import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.net.Packet;
import megamek.server.Server;

import java.util.Objects;

public class ReceiveEntityMountedFacingChanger {
    /**
     * receive and process an entity mounted facing change packet
     *
     * @param server
     * @param c the packet to be processed
     * @param connIndex the id for connection that received the packet.
     */
    public static void receiveEntityMountedFacingChange(Server server, Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        int equipId = c.getIntValue(1);
        int facing = c.getIntValue(2);
        Entity e = server.getGame().getEntity(entityId);
        if (!Objects.equals(e.getOwner(), server.getPlayer(connIndex))) {
            return;
        }
        Mounted m = e.getEquipment(equipId);

        if (m == null) {
            return;
        }
        m.setFacing(facing);
    }
}
