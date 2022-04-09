package megamek.server.receiver;

import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.net.Packet;
import megamek.server.Server;

import java.util.Objects;

public class ReceiveEntitySystemModeChange {
    /**
     * receive and process an entity system mode change packet
     *
     * @param server
     * @param c the packet to be processed
     * @param connIndex the id for connection that received the packet.
     */
    public static void receiveEntitySystemModeChange(Server server, Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        int equipId = c.getIntValue(1);
        int mode = c.getIntValue(2);
        Entity e = server.getGame().getEntity(entityId);
        if (!Objects.equals(e.getOwner(), server.getPlayer(connIndex))) {
            return;
        }
        if ((e instanceof Mech) && (equipId == Mech.SYSTEM_COCKPIT)) {
            ((Mech) e).setCockpitStatus(mode);
        }
    }
}
