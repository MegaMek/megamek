package megamek.server.Connectivity;

import megamek.MMConstants;
import megamek.common.commandline.AbstractCommandLineParser;
import megamek.common.commandline.AbstractCommandLineParser.ParseException;
import org.apache.logging.log4j.LogManager;

public class PortValidator {
    /**
     *
     * @param port if 0 or less, will return default, if illegal number, throws ParseException
     * @return valid port number
     */
    public static int validatePort(int port) throws AbstractCommandLineParser.ParseException {
        if (port <= 0) {
            return MMConstants.DEFAULT_PORT;
        }

        if ((port < MMConstants.MIN_PORT) || (port > MMConstants.MAX_PORT)) {
            String msg = String.format("Port number %d outside allowed range %d-%d", port, MMConstants.MIN_PORT, MMConstants.MAX_PORT);
            LogManager.getLogger().error(msg);
            throw new ParseException(msg);

        }
        return port;
    }
}
