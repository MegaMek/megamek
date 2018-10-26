package megamek.common.net;

import megamek.common.IPlayer;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.LogLevel;
import megamek.common.logging.MMLogger;
import megamek.server.ConnectionHandler;
import megamek.server.Server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer implements Runnable {
    private MMLogger logger = null;
    private ServerSocket serverSocket = null;
    private Thread listenThread = null;
    private ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void connected(ConnectedEvent e) {
            onConnectionOpen(e.getConnection());
        }

        @Override
        public void disconnected(DisconnectedEvent e) {
            onConnectionClose(e.getConnection());
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
        onListen();
    }

    protected int getLocalPort() {
        if (serverSocket == null) {
            return 0;
        }

        return serverSocket.getLocalPort();
    }

    protected void stopNetwork() throws Exception {

    }

    protected void onListen() {

    }

    protected void onListenEnded() {

    }

    protected void onConnectionOpen(IConnection connection) {

    }

    protected void onConnectionClose(IConnection connection) {

    }

    protected void onPacketReceived(IConnection connection, Packet packet) {

    }

    protected void onError(IConnection connection, Exception exception) {

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

                connection.addConnectionListener(connectionListener);

                onConnectionOpen(connection);
            } catch (InterruptedIOException iioe) {
                // ignore , just SOTimeout blowing..
            } catch (IOException ex) {

            }
        }
    }
}
