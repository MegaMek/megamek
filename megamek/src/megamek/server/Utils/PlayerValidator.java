package megamek.server.Utils;

import megamek.common.commandline.AbstractCommandLineParser.ParseException;
import org.apache.logging.log4j.LogManager;

public class PlayerValidator {
    /**
     *
     * @param playerName throw ParseException if null or empty
     * @return valid playerName
     */
    public static String validatePlayerName(String playerName) throws ParseException {
        if (playerName == null) {
            String msg = "playerName must not be null";
            LogManager.getLogger().error(msg);
            throw new ParseException(msg);
        }

        if (playerName.isBlank()) {
            String msg = "playerName must not be empty string";
            LogManager.getLogger().error(msg);
            throw new ParseException(msg);
        }

        return playerName.trim();
    }
}
