package megamek.common.util;


public  class ServerCommandLineParser extends AbstractCommandLineParser {
    private String gameFilename;
    private int port;
    private String password;
    private String announceUrl = "";
    private String mailProperties;

    // Options
    private static final String OPTION_PORT = "port";
    private static final String OPTION_PASSWORD = "password";
    private static final String OPTION_ANNOUNCE = "announce";
    private static final String OPTION_MAIL = "mail";

    public ServerCommandLineParser(String[] args) {
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

    public String getMailProperties() {
        return mailProperties;
    }

    /**
     * @return the game file name option value or <code>null</code> if it wasn't set
     */
    public String getGameFilename() {
        return gameFilename;
    }

    @Override
    protected void start() throws ParseException {
        while (hasNext()) {
            int tokType = getToken();
            switch (tokType) {
                case TOK_OPTION:
                    switch (getTokenValue()) {
                        case OPTION_PORT:
                            nextToken();
                            parsePort();
                            break;
                        case OPTION_ANNOUNCE:
                            nextToken();
                            parseAnnounce();
                            break;
                        case OPTION_PASSWORD:
                            nextToken();
                            parsePassword();
                            break;
                        case OPTION_MAIL:
                            nextToken();
                            parseMail();
                            break;
                    }
                    break;
                case TOK_LITERAL:
                    gameFilename = getTokenValue();
                    nextToken();
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

    private void parseAnnounce() throws ParseException {
        if (getToken() == TOK_LITERAL) {
            announceUrl = getTokenValue();
        } else {
            throw new ParseException("meta server announce URL expected");
        }
    }

    private void parsePassword() throws ParseException {
        if (getToken() == TOK_LITERAL) {
            password = getTokenValue();
        } else {
            throw new ParseException("password expected");
        }
    }

    private void parseMail() throws ParseException {
        if (getToken() == TOK_LITERAL) {
            mailProperties = getTokenValue();
        } else {
            throw new ParseException("mail properties expected");
        }
    }

}