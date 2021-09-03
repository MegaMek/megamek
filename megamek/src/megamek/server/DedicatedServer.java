/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.server;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import megamek.MegaMek;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.AbstractCommandLineParser;
import megamek.common.util.EmailService;

public class DedicatedServer {
    private static final String INCORRECT_ARGUMENTS_MESSAGE = "Incorrect arguments:";
    private static final String ARGUMENTS_DESCRIPTION_MESSAGE = "Arguments syntax:\n\t "
            + "[-password <pass>] [-port <port>] [-mail <javamail.properties>] [<saved game>]";

    public static void start(String[] args) {
        CommandLineParser cp = new CommandLineParser(args);
        try {
            cp.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            MegaMek.getLogger().error(INCORRECT_ARGUMENTS_MESSAGE + e.getMessage() + '\n'
                            + ARGUMENTS_DESCRIPTION_MESSAGE);
        }

        String saveGameFileName = cp.getGameFilename();
        int usePort;
        if (cp.getPort() != -1) {
            usePort = cp.getPort();
        } else {
            usePort = PreferenceManager.getClientPreferences().getLastServerPort();
        }
        String announceUrl = cp.getAnnounceUrl();
        String password = cp.getPassword();

        EmailService mailer = null;
        if (cp.getMailProperties() != null) {
            File propsFile = new File(cp.getMailProperties());
            try (var propsReader = new FileReader(propsFile)) {
                var mailProperties = new Properties();
                mailProperties.load(propsReader);
                mailer = new EmailService(mailProperties);
            } catch (Exception ex) {
                MegaMek.getLogger().error(
                    "Error: could not load mail properties file \"" +
                    propsFile.getAbsolutePath() + "\"", ex);
                return;
            }
        }

        // kick off a RNG check
        megamek.common.Compute.d6();
        // start server
        Server dedicated;
        try {
            if (password == null || password.length() == 0) {
                password = PreferenceManager.getClientPreferences().getLastServerPass();
            }
            dedicated = new Server(password, usePort, !announceUrl.equals(""), announceUrl, mailer);
        } catch (Exception ex) {
            MegaMek.getLogger().error("Error: could not start server at localhost" + ":" + usePort + " ("
                                      + ex.getMessage() + ").");
            return;
        }
        if (null != saveGameFileName) {
            dedicated.loadGame(new File(saveGameFileName));
        }
    }

    public static void main(String[] args) {
        start(args);
    }

    private static class CommandLineParser extends AbstractCommandLineParser {
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

        public CommandLineParser(String[] args) {
            super(args);
        }

        /**
         *
         * @return port option value or <code>-1</code> if it wasn't set
         */
        public int getPort() {
            return port;
        }
        
        /**
         * 
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
         *
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
}
