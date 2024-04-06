/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.commandline;

import java.io.File;

import org.apache.logging.log4j.LogManager;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.services.Validation;

public class ClientServerCommandLineParser extends AbstractCommandLineParser {

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

    @Override
    public String help() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s %s\n", Messages.getString("MegaMek.Version"), MMConstants.VERSION));
        sb.append(String.format("Help for %s\n", parent));
        for (ClientServerCommandLineFlag flag : ClientServerCommandLineFlag.values()) {
            if ((flag.isClientArg() && client) || (flag.isServerArg() && server) || (flag.isHostArg() && host)) {
                sb.append(String.format("-%s %s\n", flag.toString().toLowerCase(), flag.getHelpText()));
            }
        }
        return sb.toString();
    }

    @Override
    protected void start() throws ParseException {
        while (getTokenType() != TOK_EOF) {
            int tokenType = getTokenType();
            final String value = getTokenValue();
            switch (tokenType) {
                case TOK_OPTION:
                    try {
                        switch (ClientServerCommandLineFlag.parseFromString(value)) {
                            case HELP:
                                System.out.println(help());
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
                        LogManager.getLogger().error("Incorrect arguments:" + ex.getMessage() + '\n' + help());
                        throw ex;
                    }
                    break;
                case TOK_LITERAL:
                    saveGameFileName = value;
                    nextToken();
                    break;
                case TOK_EOF:
                    // Do nothing, although this shouldn't happen
                    break;
                default:
                    throw new ParseException(String.format("Unexpected input %s", value));
            }
            nextToken();
        }
    }

    private void parsePort() throws ParseException {
        if (getTokenType() == TOK_LITERAL) {
            int newPort = -1;
            try {
                newPort = Integer.decode(getTokenValue());
            } catch (NumberFormatException ex) {
                throw new ParseException(String.format(
                        "port number must be a number. '%s' is not valid\n%s",
                        getTokenValue(), ex.getMessage()));
            }
            port = Validation.validatePort(newPort);
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
            password = Validation.validatePassword(getTokenValue());
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
            playerName = Validation.validatePlayerName(getTokenValue());
        } else {
            throw new ParseException("playerName expected");
        }
    }

    private void parseServerAddress() throws ParseException {
        if (getTokenType() == TOK_LITERAL) {
            serverAddress = Validation.validateServerAddress(getTokenValue());
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

    public Resolver getResolver(String defaultPassword, int defaultPort,
                                String defaultServerAddress, String defaultPlayerName) {
        return new Resolver(this, defaultPassword, defaultPort, defaultServerAddress, defaultPlayerName);
    }

    public class Resolver {
        public final String playerName, serverAddress, password, announceUrl, mailPropertiesFile;
        public final boolean registerServer;
        public final int port;
        protected final String saveGameFileName;

        public Resolver(ClientServerCommandLineParser parser, String defaultPassword, int defaultPort,
                        String defaultServerAddress, String defaultPlayerName)
        {
            try {
                parser.parse();
            } catch (AbstractCommandLineParser.ParseException e) {
                LogManager.getLogger().error("Incorrect arguments:" + e.getMessage() + '\n' + parser.help());
            }

            String playerName = parser.getPlayerName();
            String serverAddress = parser.getServerAddress();
            int port = parser.getPort();
            String password = parser.getPassword();
            String saveGameFileName = parser.getSaveGameFileName();
            String announceUrl = parser.getAnnounceUrl();
            boolean registerServer = parser.getRegister();
            String mailPropertiesFile = parser.getMailProperties();

            // always fallback to last used player name
            if ((playerName == null) || playerName.isBlank()) {
                playerName = defaultPlayerName;
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
                port = MMConstants.DEFAULT_PORT;
            }

            if ((playerName == null) || playerName.isBlank()) {
                playerName = MMConstants.DEFAULT_PLAYERNAME;
            }

            if ((serverAddress == null) || serverAddress.isBlank()) {
                serverAddress = MMConstants.LOCALHOST;
            }

            if ((password != null) && password.isBlank()) {
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

        /**
         * path resolves the saveGameFileName arg if it was provided.
         * If it is an absolute path, uses that, otherwise looks first in
         * current working directory and then in ./savegame/
         * @return File object if valid file, null if not
         */
        public File getSaveGameFile() {
            if (saveGameFileName == null) {
                return null;
            }
            File gameFile = new File(saveGameFileName);

            if (gameFile.canRead()) {
                return gameFile;
            } else if (gameFile.isAbsolute()) {
                LogManager.getLogger().error("Unable to read savegame file: " + gameFile.getAbsolutePath());
                return null;
            } else {
                String searched = '"'+ gameFile.getAbsolutePath()+'"';
                gameFile = new File("./savegames", saveGameFileName);
                if (gameFile.canRead()) {
                    return gameFile;
                } else {
                    searched += " or \"" + gameFile.getAbsolutePath()+'"';
                    LogManager.getLogger().error("Unable to read savegame file at " + searched);
                    return null;
                }
            }
        }
    }
}