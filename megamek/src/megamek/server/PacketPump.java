package megamek.server;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PacketPump implements Runnable {

    private boolean shouldStop;
    private ConcurrentLinkedQueue<ReceivedPacket> packetQueue;
    private Object serverLock;
    private Server server;

    PacketPump(ConcurrentLinkedQueue<ReceivedPacket> packetQueue, Object serverLock, Server server) {
        shouldStop = false;
        this.packetQueue = packetQueue;
        this.serverLock = serverLock;
        this.server  = server;
    }

    void signalEnd() {
        shouldStop = true;
    }

    @Override
    public void run() {
        while (!shouldStop) {
            while (!packetQueue.isEmpty()) {
                ReceivedPacket rp = packetQueue.poll();
                synchronized (serverLock) {
                    server.handle(rp.connId, rp.packet);
                }
            }
            try {
                synchronized (packetQueue) {
                    packetQueue.wait();
                }
            } catch (InterruptedException e) {
                // If we are interrupted, just keep going, generally
                // this happens after we are signalled to stop.
            }
        }
    }

}
