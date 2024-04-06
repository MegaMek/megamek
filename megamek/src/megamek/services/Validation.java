package megamek.services;

import org.apache.logging.log4j.LogManager;

import megamek.MMConstants;
import megamek.codeUtilities.StringUtility;
import megamek.common.annotations.Nullable;
import megamek.common.commandline.AbstractCommandLineParser.ParseException;

public class Validation {
    
    /**
     * @param serverAddress
     * @return valid hostName
     * @throws ParseException for null or empty serverAddress
     */
    public static String validateServerAddress(String serverAddress) throws ParseException {
        if ((serverAddress == null) || serverAddress.isBlank()) {
            String msg = "serverAddress must not be null or empty";
            LogManager.getLogger().error(msg);
            throw new ParseException(msg);
        } else {
            return serverAddress.trim();
        }
    }

    /**
     * @param playerName throw ParseException if null or empty
     * @return valid playerName
     */
    public static String validatePlayerName(String playerName) throws ParseException {
        if (playerName == null) {
            String msg = "playerName must not be null";
            LogManager.getLogger().error(msg);
            throw new ParseException(msg);
        } else if (playerName.isBlank()) {
            String msg = "playerName must not be empty string";
            LogManager.getLogger().error(msg);
            throw new ParseException(msg);
        } else {
            return playerName.trim();
        }
    }

    /**
     * @param password
     * @return valid password or null if no password or password is blank string
     */
    public static @Nullable String validatePassword(@Nullable String password) {
        return StringUtility.isNullOrBlank(password) ? null : password.trim();
    }

    /**
     * @param port if 0 or less, will return default, if illegal number, throws ParseException
     * @return valid port number
     */
    public static int validatePort(int port) throws ParseException {
        if (port <= 0) {
            return MMConstants.DEFAULT_PORT;
        } else if ((port < MMConstants.MIN_PORT) || (port > MMConstants.MAX_PORT)) {
            String msg = String.format("Port number %d outside allowed range %d-%d", port, MMConstants.MIN_PORT, MMConstants.MAX_PORT);
            LogManager.getLogger().error(msg);
            throw new ParseException(msg);
        } else {
            return port;
        }
    }

}
