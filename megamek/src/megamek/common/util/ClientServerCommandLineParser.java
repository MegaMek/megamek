package megamek.common.util;

import megamek.MMConstants;
import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Locale;

public  class ClientServerCommandLineParser extends AbstractCommandLineParser {

    private static final String INCORRECT_ARGUMENTS_MESSAGE = "Incorrect arguments:";

    public enum ClientServerCommandLineFlag {
        //region Enum Declarations
        HELP(Messages.getString("MegaMek.Help")),
        USEDEFAULTS(Messages.getString("MegaMek.Help.UseDefaults")),
        PORT(Messages.getFormattedString("MegaMek.Help.Port", Server.MIN_PORT, Server.MAX_PORT, Server.DEFAULT_PORT)),
        DATADIR(Messages.getFormattedString("MegaMek.Help.DataDir",  Configuration.dataDir())),
        // server or host only options
        ANNOUNCE(Messages.getString("MegaMek.Help.Announce"), true, false, true),
        MAIL(Messages.getString("MegaMek.Help.Mail"), true, false, true),
        SAVEGAME(Messages.getString("MegaMek.Help.SaveGame"), true, false, true),
        PASSWORD(Messages.getString("MegaMek.Help.Password"), true, false, true),
        // client or host only options
        PLAYERNAME(Messages.getString("MegaMek.Help.PlayerName"), false, true, true),
        // client only options
        SERVER(Messages.getFormattedString("MegaMek.Help.Server", Server.LOCALHOST), false, true, false),
        ;
        //endregion Enum Declarations

        private final String helpText;
        private final boolean server;
        private final boolean client;
        private final boolean host;

        //region Constructors
        ClientServerCommandLineFlag(final String helpText) {
            this(helpText, true, true, true);
        }

        ClientServerCommandLineFlag(final String helpText, boolean server, boolean client, boolean host) {
            this.helpText = helpText; //resources.getString(helpText);
            this.server = server;
            this.client = client;
            this.host = host;
        }

        public static ClientServerCommandLineFlag parseFromString(final String text) {
            try {
                return valueOf(text.toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                LogManager.getLogger().error("Failed to parse the ClientServerCommandLineFlag from '%s' ", text);
                throw(ex);
            }
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    private String saveGameFileName;
    private int port;
    private boolean useDefaults;
    private String password;
    private String announceUrl;
    private String mailProperties;
    private String serverAddress;
    private String playerName;

    private final String parent;
    private final boolean server;
    private final boolean client;
    private final boolean host;

    public ClientServerCommandLineParser(String[] args, String parent, boolean server, boolean client, boolean host) {
        super(args);
        this.server = server;
        this.client = client;
        this.host = host;
        this.parent = parent;
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
    @Nullable
    public String getPassword() {
        return password;
    }

    @Nullable
    public String getAnnounceUrl() {
        return announceUrl;
    }

    public boolean getRegister() {
        return (announceUrl != null) && (!announceUrl.isBlank());
    }

    @Nullable
    public String getMailProperties() {
        return mailProperties;
    }

    @Nullable
    public String getPlayerName() {
        return playerName;
    }

    @Nullable
    public String getServerAddress() {
        return serverAddress;
    }

    public boolean getUseDefaults() {
        return useDefaults;
    }
    /**
     * @return the game file name option value or <code>null</code> if it wasn't set
     */
    public String getSaveGameFileName() {
        return saveGameFileName;
    }

    public String help() {
        StringBuilder sb = new StringBuilder();
        sb.append(Messages.getString("MegaMek.Version") + MMConstants.VERSION+"\n");
        sb.append(String.format("Help for %s\n", parent));
        for( ClientServerCommandLineFlag flag : ClientServerCommandLineFlag.values() ) {
            if ( (flag.client && client) || (flag.server && server) || (flag.host && host)  ) {
                sb.append(String.format("-%s %s\n", flag.toString().toLowerCase(), flag.helpText));
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
            int tokenType = getTokenType();
            final String tokenValue = getTokenValue();
            switch (tokenType) {
                case TOK_OPTION:
                    try {
                        switch ( ClientServerCommandLineFlag.parseFromString(tokenValue)) {
                            case HELP:
                                MegaMek.printToOut(help());
                                System.exit(0);
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
                            case SERVER:
                                nextToken();
                                parseServerAddress();
                                break;
                            case SAVEGAME:
                                nextToken();
                                parseSaveGame();
                                break;
                            case DATADIR:
                                nextToken();
                                processDataDir();
                                break;
                            case USEDEFAULTS:
                                useDefaults = true;
                                break;
                        }
                    } catch (ParseException ex) {
                        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
                        out.print(formatErrorMessage(ex));
                        out.close();
                        MegaMek.printToOut(help());
                        throw ex;
                    }
                    break;
                case TOK_LITERAL:
                    // this is old behavior, but it didn't seem to work
                    // It works now and I left it in just in case
                    saveGameFileName = tokenValue;
                    nextToken();
                    break;
                case TOK_EOF:
                    // Do nothing, although this shouldn't happen
                    break;
                default:
                    throw new ParseException(String.format("Unexpected input %s",tokenValue));
            }
            nextToken();
        }
    }

    private void parsePort() throws ParseException {
        if (getTokenType() == TOK_LITERAL) {
            int newPort = -1;
            newPort = Integer.decode(getTokenValue());
            port = Server.validatePort(newPort);
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
            password = Server.validatePassword(getTokenValue());
        } else {
            //throw new ParseException("password expected");
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
            playerName = Server.validatePlayerName(getTokenValue());
        } else {
            throw new ParseException("playerName expected");
        }
    }

    private void parseServerAddress() throws ParseException {
        if (getTokenType() == TOK_LITERAL) {
            serverAddress = Server.validateServerAddress(getTokenValue());
        } else {
            throw new ParseException("host name or url expected");
        }
    }

    private void parseSaveGame() throws ParseException {
        if (getTokenType() == TOK_LITERAL) {
            saveGameFileName = getTokenValue();
        } else {
            throw new ParseException("Saved game filename expected");
        }
    }

    private void processDataDir() throws ParseException {
        String dataDirName;
        if (getTokenType() == TOK_LITERAL) {
            dataDirName = getTokenValue();
            nextToken();
            Configuration.setDataDir(new File(dataDirName));
        } else {
            throw new ParseException("directory name expected");
        }
    }

    public Resolver getResolver(String defaultPassword, int defaultPort, String defaultServerAddress) {
        return new Resolver(this, defaultPassword, defaultPort, defaultServerAddress);
    }

    public class Resolver {
        public final String playerName, serverAddress, password, saveGameFileName, announceUrl, mailPropertiesFile;
        public final boolean registerServer;
        public final int port;

        public Resolver(ClientServerCommandLineParser parser, String defaultPassword, int defaultPort, String defaultServerAddress)
        {
            try {
                parser.parse();
            } catch (AbstractCommandLineParser.ParseException e) {
                LogManager.getLogger().error(parser.formatErrorMessage(e));
            }

            String playerName = parser.getPlayerName();
            String serverAddress = parser.getServerAddress();
            int port = parser.getPort();
            String password = parser.getPassword();
            String saveGameFileName = parser.getSaveGameFileName();
            String announceUrl = parser.getAnnounceUrl();
            boolean registerServer = parser.getRegister();
            String mailPropertiesFile = parser.getMailProperties();

            //always fallback to last used player name
            if (playerName == null || playerName.isBlank()) {
                playerName = PreferenceManager.getClientPreferences().getLastPlayerName();
            }

            if (!parser.getUseDefaults()) {
                if (password == null) {
                    password = defaultPassword;
                }

                if (port <= 0) {
                    port = defaultPort;
                }

                if (serverAddress == null) {
                    serverAddress = defaultServerAddress;
                }
            }

            // use hard-coded defaults if not otherwise defined
            if (port <= 0) {
                port = Server.DEFAULT_PORT;
            }

            if (playerName == null || playerName.isBlank()) {
                playerName = Server.DEFAULT_PLAYERNAME;
            }

            if (serverAddress == null || serverAddress.isBlank()) {
                serverAddress = Server.LOCALHOST;
            }

            if (password != null && password.isBlank()) {
                password = null;
            }

            this.playerName = playerName;
            this.serverAddress = serverAddress;
            this.port = port;
            this.password = password;
            this.saveGameFileName = saveGameFileName;
            this.announceUrl = announceUrl;
            this.registerServer = registerServer;
            this.mailPropertiesFile = mailPropertiesFile;
        }
    }
}