package megamek.server;

import megamek.common.net.Packet;

public class ReceivedPacket {

        public int connId;
        public Packet packet;

        ReceivedPacket(int cid, Packet p) {
            packet = p;
            connId = cid;
        }
}
