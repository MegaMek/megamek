package megamek.server.receiver;

import megamek.common.Player;
import megamek.common.net.Packet;
import megamek.server.Server;

public class ReceiveEntityPlayerDoner {
    /**
     * Sets a player's ready status
     */
    public static void receivePlayerDone(Server server, Packet pkt, int connIndex) {
        boolean ready = pkt.getBooleanValue(0);
        Player player = server.getPlayer(connIndex);
        if (null != player) {
            player.setDone(ready);
        }
    }
}
