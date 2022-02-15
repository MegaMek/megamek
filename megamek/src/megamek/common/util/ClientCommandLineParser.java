package megamek.common.util;

import megamek.MegaMek;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public  class ClientCommandLineParser extends AbstractCommandLineParser {

    public enum ServerCommandLineFlag {
        //region Enum Declarations
        PORT("port","set which port the server listens to. Defautls to "+ Server.DEFAULT_PORT),
        PASSWORD("password","set which port the server listens to. Defautls to "+ Server.DEFAULT_PORT),
        HOST("announce","set which port the server listens to. Defautls to "+ Server.DEFAULT_PORT),
        PLAYERNAME("mail","set which port the server listens to. Defautls to "+ Server.DEFAULT_PORT);
        //endregion Enum Declarations

        private final String name;
        private final String toolTipText;

        //region Constructors
        ServerCommandLineFlag(final String name, final String toolTipText) {
            //            final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
            //                    MegaMek.getMekHQOptions().getLocale(), new EncodeControl());
            this.name = name;
            this.toolTipText = toolTipText; //resources.getString(toolTipText);
        }
        //endregion Constructors
        //region File I/O
        /**
         * This allows for the legacy parsing method of financial durations, outdated in 0.49.X
         */
        public static ServerCommandLineFlag parseFromString(final String text) {
            try {
                return valueOf(text.toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                LogManager.getLogger().error("Failed to parse the ServerCommandLineFlag from text " + text);
                throw(ex);
            }
        }
        //endregion File I/O

        @Override
        public String toString() {
            return name;
        }
    }

    private String host;
    private int port;
    private String password;
    private String playerName = "";

    public ClientCommandLineParser(String[] args) {
        super(args);
    }

    /**
     * @return port option value or <code>-1</code> if it wasn't set
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the password option value, will be null if not set.
     */
    public String getPassword() {
        return password;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getHost() {
        return host;
    }

    @Override
    protected void start() throws ParseException {
        while (hasNext()) {
            int tokType = getToken();
            switch (tokType) {
                case TOK_OPTION:
                    try {
                        switch ( ServerCommandLineFlag.parseFromString(getTokenValue())) {
                            case PORT:
                                nextToken();
                                parsePort();
                                break;
                            case HOST:
                                nextToken();
                                parseHost();
                                break;
                            case PASSWORD:
                                nextToken();
                                parsePassword();
                                break;
                            case PLAYERNAME:
                                nextToken();
                                parsePlayerName();
                                break;
                        }
                    } catch (Exception ex) {
                        //ignore or fail?
                    }
                    break;
                case TOK_EOF:
                    // Do nothing, although this shouldn't happen
                    break;
                default:
                    throw new ParseException("unexpected input");
            }
            nextToken();
        }
    }

    private void parsePort() throws ParseException {
        if (getToken() == TOK_LITERAL) {
            int newPort = -1;
            try {
                newPort = Integer.decode(getTokenValue());
            } catch (NumberFormatException ignored) {
                //ignore, leave at -1
            }
            if ((newPort < 0) || (newPort > 65535)) {
                throw new ParseException("invalid port number");
            }
            port = newPort;
        } else {
            throw new ParseException("port number expected");
        }
    }

    private void parsePlayerName() throws ParseException {
        if (getToken() == TOK_LITERAL) {
            playerName = getTokenValue();
        } else {
            throw new ParseException("playerName expected");
        }
    }

    private void parsePassword() throws ParseException {
        if (getToken() == TOK_LITERAL) {
            password = getTokenValue();
        } else {
            throw new ParseException("password expected");
        }
    }

    private void parseHost() throws ParseException {
        if (getToken() == TOK_LITERAL) {
            host = getTokenValue();
        } else {
            throw new ParseException("mail properties expected");
        }
    }
}