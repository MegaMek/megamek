package megamek.server;

import megamek.common.net.packets.Packet;

public abstract class AbstractGameManager implements IGameManager {


    protected final void send(Packet p) {
        Server.getServerInstance().send(p);
    }

    protected final void send(int connId, Packet p) {
        Server.getServerInstance().send(connId, p);
    }
}
