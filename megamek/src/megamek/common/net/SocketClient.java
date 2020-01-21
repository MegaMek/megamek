package megamek.common.net;

public class SocketClient {

    ConnectionHandler packetUpdate;
    private Thread connThread;
    private IConnection connection;
    private boolean connected = false;

    private ConnectionListenerAdapter connectionListener = new ConnectionListenerAdapter() {

        /**
         * Called when it is sensed that a connection has terminated.
         */
        @Override
        public void disconnected(DisconnectedEvent e) {
            onRemoteDisconnection();
        }

        @Override
        public void packetReceived(final PacketReceivedEvent e) {
            onPacketReceived(e.getPacket());
        }

    };

    /**
     * Stub implementation of onPacketReceived(); subclass should
     * override this method to handle incoming packets.
     * @Note This method will be invoked on a separate thread, so subclass
     *       must handle any thread synchronization issues.
     * @param packet
     */
    protected void onPacketReceived(Packet packet) {
    }

    /**
     * Stub implementation of onRemoteDisconnection(); subclass should
     * override this method to handle disconnection initiated by remote endpoint
     * such as server.
     * @Note This method will be invoked on a separate thread, so subclass
     *       must handle any thread synchronization issues.
     */
    protected void onRemoteDisconnection() {
    }

    private class ConnectionHandler implements Runnable {

        boolean shouldStop = false;

        public void signalStop() {
            shouldStop = true;
        }

        public void run() {
            while (!shouldStop) {
                // Write any queued packets
                flushConn();
                // Wait for new input
                updateConnection();
                if ((connection == null) || connection.isClosed()) {
                    shouldStop = true;
                }
            }
        }
    }

    /**
     * call this once to update the connection
     */
    protected void updateConnection() {
        if (connection != null && !connection.isClosed()) {
            connection.update();
        }
    }

    protected boolean connect(String hostname, int port) {
        connection = ConnectionFactory.getInstance().createClientConnection(hostname, port, 1);
        boolean result = connection.open();
        if (result) {
            connection.addConnectionListener(connectionListener);
            packetUpdate = new ConnectionHandler();
            connThread = new Thread(packetUpdate, "Client Connection, Player " + getName());
            connThread.start();
        }

        return result;
    }

    protected boolean isConnected() {
        return connected;
    }

    protected void serverHandshakeCompleted() {
        connected = true;
    }

    protected String getName() {
        return "(Unknown)";
    }

    protected void disconnect() {
        // If we're still connected, tell the server that we're going down.
        if (connected) {
            // Stop listening for in coming packets, this should be done before
            // sending the close connection command
            packetUpdate.signalStop();
            connThread.interrupt();
            send(new Packet(Packet.COMMAND_CLOSE_CONNECTION));
            flushConn();
        }
        connected = false;

        if (connection != null) {
            connection.close();
        }

    }

    /**
     * send the message to the server
     */
    protected void send(Packet packet) {
        if (connection != null) {
            connection.send(packet);
        }
    }

    /**
     * send all buffered packets on their way this should be called after
     * everything which causes us to wait for a reply. For example "done" button
     * presses etc. to make stuff more efficient, this should only be called
     * after a batch of packets is sent,not separately for each packet
     */
    protected void flushConn() {
        if (connection != null) {
            connection.flush();
        }
    }
}
