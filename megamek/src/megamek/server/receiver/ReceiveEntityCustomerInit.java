package megamek.server.receiver;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.net.Packet;
import megamek.server.Server;

public class ReceiveEntityCustomerInit {
    /**
     *
     * @param server
     * @param c the packet to be processed
     */
    public static void receiveCustomInit(Server server, Packet c) {
        // In the chat lounge, notify players of customizing of unit
        if (server.getGame().getPhase() == GamePhase.LOUNGE) {
            Player p = (Player) c.getObject(0);
            server.sendServerChat("" + p.getName() + " has customized initiative.");
        }
    }
}
