package megamek.common.net;

import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.LogLevel;
import megamek.common.logging.MMLogger;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

public class SocketServer implements Runnable {
    private MMLogger logger = null;
    private ServerSocket serverSocket = null;
    private Thread listenThread = null;
    private Hashtable<Integer, ConnectionHandler> connectionHandlers = new Hashtable<>();

    /**
     * Called after the server is in a state ready to accept new incoming connections.
     * @Note: This method is a stub and is intended to be overridden by subclasses that
     *        wish to be notified of this event
     */
    protected void onListen() {
    }

    /**
     * Called after the server is no longer in a condition to accept new incoming connections (typically
     * when the server is being shut down).
     * @Note: This method is a stub and is intended to be overridden by subclasses that
     *        wish to be notified of this event
     */
    protected void onListenEnded() {
    }

    /**
     * Called when a new incoming connection has been accepted. At the time this method is invoked, the
     * connection is not fully initialized (for example, any unique connection ID should be set by the
     * subclass override of this method).
     * @param connection The new incoming connection.
     * @Note: This method is a stub and is intended to be overridden by subclasses that
     *        wish to be notified of this event
     * @Note: This method will be invoked from a different thread and it is the responsibility
     *        of the subclass to manage any cross-thread synchronization needed.
     */
    protected void onConnectionOpen(IConnection connection) {
    }

    /**
     * Called when an existing connection has closed (when the remote client initiates the disconnection). At
     * the time that this method is invoked, the connection has been closed and any messages sent on the connection
     * will fail to reach the intended destination.
     * @param connection The connection that has been closed.
     * @Note: This method is a stub and is intended to be overridden by subclasses that
     *        wish to be notified of this event
     * @Note: This method will be invoked from a different thread and it is the responsibility
     *        of the subclass to manage any cross-thread synchronization needed.
     */
    protected void onConnectionClose(IConnection connection) {
    }

    /**
     * Called when a packet arrives on an existing connection.
     * @param connection The connection receiving the packet.
     * @param packet The packet that arrived on the connection.
     * @Note: This method is a stub and is intended to be overridden by subclasses that
     *        wish to be notified of this event
     * @Note: This method will be invoked from a different thread and it is the responsibility
     *        of the subclass to manage any cross-thread synchronization needed.
     */
    protected void onPacketReceived(IConnection connection, Packet packet) {
    }

    private ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void connected(ConnectedEvent e) {
        }

        @Override
        public void disconnected(DisconnectedEvent e) {
            IConnection conn = e.getConnection();
            onConnectionClose(conn);

            ConnectionHandler ch = connectionHandlers.get(conn.getId());
            if (ch != null) {
                ch.signalStop();
                connectionHandlers.remove(conn.getId());
            }
        }

        @Override
        public void packetReceived(PacketReceivedEvent e) {
            onPacketReceived(e.getConnection(), e.getPacket());
        }
    };

    private MMLogger getLogger() {
        if (null == logger) {
            logger = DefaultMmLogger.getInstance();
        }

        return logger;
    }

    private void logInfo(String methodName, String message) {
        getLogger().log(getClass(), methodName, LogLevel.INFO, message);
    }

    protected void listen(int port) throws IOException {
        serverSocket = new ServerSocket(port);

        listenThread = new Thread(this, "Socket Server");
        listenThread.start();
        onListen();
    }

    protected int getLocalPort() {
        if (serverSocket == null) {
            return 0;
        }

        return serverSocket.getLocalPort();
    }

    protected void stopNetwork() throws Exception {
        serverSocket.close();
        onListenEnded();
    }

    @Override
    public void run() {
        final String METHOD_NAME = "run()";
        logInfo(METHOD_NAME, "s: listening for clients...");

        Thread currentThread = Thread.currentThread();
        while (listenThread == currentThread) {
            try {
                Socket s = serverSocket.accept();

                IConnection connection = ConnectionFactory.getInstance()
                        .createServerConnection(s, 0);

                // call out to subclass; this should set a valid ID on the connection
                onConnectionOpen(connection);

                connection.addConnectionListener(connectionListener);

                ConnectionHandler ch = new ConnectionHandler(connection);
                Thread newConnThread = new Thread(ch, "Connection " + connection.getId());
                newConnThread.start();
                connectionHandlers.put(connection.getId(), ch);
            } catch (InterruptedIOException iioe) {
                // ignore , just SOTimeout blowing..
            } catch (IOException ex) {

            }
        }
    }
}
