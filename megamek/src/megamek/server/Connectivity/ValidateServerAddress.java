package megamek.server.Connectivity;

import megamek.common.commandline.AbstractCommandLineParser;
import org.apache.logging.log4j.LogManager;

public class ValidateServerAddress {
    /**
     *
     * @return valid hostName
     * @throws AbstractCommandLineParser.ParseException for null or empty serverAddress
     */
    public static String validateServerAddress(String serverAddress) throws AbstractCommandLineParser.ParseException {
        if ((serverAddress == null) || serverAddress.isBlank()) {
            String msg = "serverAddress must not be null or empty";
            LogManager.getLogger().error(msg);
            throw new AbstractCommandLineParser.ParseException(msg);
        }
        return serverAddress.trim();
    }
}
