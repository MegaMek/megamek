package megamek.common.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import megamek.common.Packet;

public class ObjectStreamConnection extends Connection {

    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    public ObjectStreamConnection(Socket socket, int id) {
        super(socket, id);
    }

    public ObjectStreamConnection(String host, int port, int id) {
        super(host, port, id);
    }

    protected Packet readPacket() throws Exception {
        if (in == null) {
            in = new ObjectInputStream(getSocket().getInputStream());
        }
        Packet packet = (Packet)in.readObject();
        return packet;
    }

    protected int sendPacket(Packet packet) throws Exception {
        int bytes = 0;
        if (out == null) {
            out = new ObjectOutputStream(getSocket().getOutputStream());
            out.flush();
        }
        out.reset(); // write each packet fresh
        out.writeObject(packet);
        out.flush();
        bytes = packet.size();
        return bytes;
    }
}
