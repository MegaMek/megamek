package megamek.common.util;

import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Locale;

public  class ClientServerCommandLineParser extends AbstractCommandLineParser {

    private static final String INCORRECT_ARGUMENTS_MESSAGE = "Incorrect arguments:";
//    private static final String ARGUMENTS_DESCRIPTION_MESSAGE = "Arguments syntax:\n\t MegaMek "
//            + "[-log <logfile>] [(-gui <guiname>)|(-dedicated)|(-validate)|(-export)|(-eqdb)|"
//            + "(-eqedb) (-oul)] [<args>]";

    public enum ServerCommandLineFlag {
        //region Enum Declarations
        HELP("print this help message"),
        PORT(String.format("Port the server listens to or the client connects to. Valid range %d - %d. Default is %d", Server.MIN_PORT, Server.MAX_PORT, Server.DEFAULT_PORT)),
        PASSWORD("Password to ??? . Default is to use last password"),
        // server or host only options
        ANNOUNCE("The url to the server announcer. Default is not to announce", true, false, true),
        MAIL("Mail ??. Default is no mail", true, false, true),
        // client or host only options
        PLAYERNAME("Name client gets in game. Default is last used name", false, true, true),
        // client only options
        JOIN(String.format("Name or URL of the server to join. Default %s", Server.LOCALHOST), false, true, false);

        //endregion Enum Declarations

        private final String toolTipText;
        private final boolean server;
        private final boolean client;
        private final boolean host;

        //region Constructors
        ServerCommandLineFlag(final String toolTipText) {
            this(toolTipText, true, true, true);
        }

        ServerCommandLineFlag(final String toolTipText, boolean server, boolean client, boolean host) {
            //            final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
            //                    MegaMek.getMekHQOptions().getLocale(), new EncodeControl());
            this.toolTipText = toolTipText; //resources.getString(toolTipText);
            this.server = server;
            this.client = client;
            this.host = host;
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
    private String hostName;
    private String playerName = "";

    private final boolean server;
    private final boolean client;
    private final boolean host;

    public ClientServerCommandLineParser(String[] args, boolean server, boolean client, boolean host) {
        super(args);
        this.server = server;
        this.client = client;
        this.host = host;
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

    public String getHostName() {
        return hostName;
    }

    /**
     * @return the game file name option value or <code>null</code> if it wasn't set
     */
    public String getGameFilename() {
        return gameFilename;
    }

    public void printHelp() {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
        out.print(help());
        out.flush();
        out.close();
    }

    public String help() {
        StringBuilder sb = new StringBuilder();
        for( ServerCommandLineFlag flag : ServerCommandLineFlag.values() ) {
            if ( (flag.client && client) || (flag.server && server) || (flag.host && host)  ) {
                sb.append(String.format("-%s %s\n", flag.toString().toLowerCase(), flag.toolTipText));
            }
        }
        return sb.toString();
    }

    @Override
    public String formatErrorMessage(ParseException e) {
        return (INCORRECT_ARGUMENTS_MESSAGE + e.getMessage() + '\n'
                + help());
    }

    @Override
    protected void start() throws ParseException {
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
                    } catch (ParseException ex) {
                        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
                        out.print(formatErrorMessage(ex));
                        out.close();
                        printHelp();
                        throw ex;
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
            if ((newPort < 1025) || (newPort > 65535)) {
                throw new ParseException(String.format("invalid port number %d, must be in range 1025 - 65535",newPort));
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
                hostName = getTokenValue();
            } else {
                throw new ParseException("mail properties expected");
            }
        }
}