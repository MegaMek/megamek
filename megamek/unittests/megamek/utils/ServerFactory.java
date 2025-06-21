package megamek.utils;

import megamek.MMConstants;
import megamek.server.AbstractGameManager;
import megamek.server.Server;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerFactory {

    /**
     * 
     * @param gameManager A server requires a GameManager instance of some type to instantiate
     * @return Server a valid server using an open port
     * @throws IOException
     */
    public static Server createServer(AbstractGameManager gameManager) throws IOException {
        
        int port = getOpenPort(MMConstants.MIN_PORT_FOR_QUICK_GAME, MMConstants.MAX_PORT);
        if (port == -1) {
            throw new IOException("No ports available in range!");
        }
        
        return new Server(
              null, port, gameManager, false, "", null, true
        );
    }

    /**
     * 
     * @param minPort Port number at which to begin searching
     * @param maxPort Port number at which to stop the search
     * @return int Port number that is currently unused
     */
    public static int getOpenPort(int minPort, int maxPort) {
        int maxIterations = maxPort - minPort;
        int port;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            port = minPort + iteration;
            if (isLocalPortFree(port)) {
                return port;
            }
        }
        return -1;
    }

    /**
     * 
     * @param port Port number to test on the local machine
     * @return boolean true if port is currently unused
     */
    public static boolean isLocalPortFree(int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
