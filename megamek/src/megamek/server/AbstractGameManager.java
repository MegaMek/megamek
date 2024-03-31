package megamek.server;

import megamek.common.enums.GamePhase;
import megamek.common.net.packets.Packet;

abstract class AbstractGameManager implements IGameManager {

    protected final GameManagerPacketHelper packetHelper = new GameManagerPacketHelper(this);

    protected final void send(Packet p) {
        Server.getServerInstance().send(p);
    }

    protected final void send(int connId, Packet p) {
        Server.getServerInstance().send(connId, p);
    }

    protected abstract void endCurrentPhase();

    protected abstract void changePhase(GamePhase newPhase);
}
