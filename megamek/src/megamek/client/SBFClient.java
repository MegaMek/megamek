package megamek.client;

import megamek.common.IGame;
import megamek.common.net.packets.Packet;

public class SBFClient extends AbstractClient {


    /**
     * Construct a client which will try to connect. If the connection fails, it
     * will alert the player, free resources and hide the frame.
     *
     * @param name the player name for this client
     * @param host the hostname
     * @param port the host port
     */
    public SBFClient(String name, String host, int port) {
        super(name, host, port);
    }

    @Override
    public boolean connect() {
        return false;
    }

    @Override
    public void die() {

    }

    @Override
    public IGame getIGame() {
        return null;
    }

    @Override
    protected boolean handleGameSpecificPacket(Packet packet) throws Exception {
        return false;
    }
}
