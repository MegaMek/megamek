package megamek.common.util;

import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Locale;

public  class ClientServerCommandLineParser extends AbstractCommandLineParser {

    public enum ServerCommandLineFlag {
        //region Enum Declarations
        PORT("set which port the server listens to or the client connects to. Default is "+ Server.DEFAULT_PORT),
        PASSWORD("Password to ??? . Default is to use last password"),
        ANNOUNCE("The url to the server announcer. Default is not to announce"),
        MAIL("Mail ??. Default is no mail"),
        PLAYERNAME("What name client gets in the lobby. Default is last used name"),
        JOIN("the name or rul of the server to join"+ Server.LOCALHOST),
        HELP("print this help message");

        //endregion Enum Declarations

        private final String toolTipText;

        //region Constructors
        ServerCommandLineFlag(final String toolTipText) {
//            final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
//                    MegaMek.getMekHQOptions().getLocale(), new EncodeControl());
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
            return super.toString();
        }
    }

    private String gameFilename;
    private int port;
    private String password;
    private String announceUrl = "";
    private String mailProperties;
    private String host;
    private String playerName = "";

    public ClientServerCommandLineParser(String[] args) {
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

    public String getAnnounceUrl() {
        return announceUrl;
    }

    public boolean getRegister() {
        return (announceUrl != null) && (!announceUrl.isBlank());
    }

    public String getMailProperties() {
        return mailProperties;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getHost() {
        return host;
    }

    /**
     * @return the game file name option value or <code>null</code> if it wasn't set
     */
    public String getGameFilename() {
        return gameFilename;
    }

    public void printHelp() {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
        out.println(String.format("HELP"));
        for( ServerCommandLineFlag flag : ServerCommandLineFlag.values() ) {
            out.println(String.format("-%s %s",flag.toString().toLowerCase(), flag.toolTipText));
        }
        out.flush();
        out.close();
    }

    @Override
    protected void start() throws ParseException {
        System.out.println(("TEST"));
        while ( getTokenType() != TOK_EOF) {
            int tokType = getTokenType();
            String tokValue = getTokenValue();
            switch (tokType) {
                case TOK_OPTION:
                    try {
                        switch ( ServerCommandLineFlag.parseFromString(tokValue)) {
                            case PORT:
                                nextToken();
                                parsePort();
                                break;
                            case ANNOUNCE:
                                nextToken();
                                parseAnnounce();
                                break;
                            case PASSWORD:
                                nextToken();
                                parsePassword();
                                break;
                            case MAIL:
                                nextToken();
                                parseMail();
                                break;
                            case PLAYERNAME:
                                nextToken();
                                parsePlayerName();
                                break;
                            case JOIN:
                                nextToken();
                                parseHost();
                                break;
                            case HELP:
                                nextToken();
                                printHelp();
                                //is it safe to exit here?
                                System.exit(0);
                        }
                    } catch (Exception ex) {
                        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
                        out.println(String.format("Unknown flag %s",tokValue));
                        out.close();
                        printHelp();
                        throw new ParseException(String.format("Unknown flag %s",tokValue));
                    }
                    break;
                case TOK_LITERAL:
                    gameFilename = tokValue;
                    nextToken();
                    break;
                case TOK_EOF:
                    // Do nothing, although this shouldn't happen
                    break;
                default:
                    throw new ParseException(String.format("Unexpected input %s",tokValue));
            }
            nextToken();
        }
    }

    private void parsePort() throws ParseException {
        if (getTokenType() == TOK_LITERAL) {
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

    private void parseAnnounce() throws ParseException {
        if (getTokenType() == TOK_LITERAL) {
            announceUrl = getTokenValue();
        } else {
            throw new ParseException("meta server announce URL expected");
        }
    }

    private void parsePassword() throws ParseException {
        if (getTokenType() == TOK_LITERAL) {
            password = getTokenValue();
        } else {
            throw new ParseException("password expected");
        }
    }

    private void parseMail() throws ParseException {
        if (getTokenType() == TOK_LITERAL) {
            mailProperties = getTokenValue();
        } else {
            throw new ParseException("mail properties expected");
        }
    }

        private void parsePlayerName() throws ParseException {
            if (getTokenType() == TOK_LITERAL) {
                playerName = getTokenValue();
            } else {
                throw new ParseException("playerName expected");
            }
        }

        private void parseHost() throws ParseException {
            if (getTokenType() == TOK_LITERAL) {
                host = getTokenValue();
            } else {
                throw new ParseException("mail properties expected");
            }
        }
}