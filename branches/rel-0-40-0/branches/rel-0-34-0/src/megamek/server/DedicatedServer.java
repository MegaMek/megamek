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
import java.io.IOException;

import megamek.common.preference.PreferenceManager;
import megamek.common.util.AbstractCommandLineParser;

public class DedicatedServer {

    private static final String INCORRECT_ARGUMENTS_MESSAGE = "Incorrect arguments:";

    private static final String ARGUMENTS_DESCRIPTION_MESSAGE = "Arguments syntax:\n\t [-port <port>] [<saved game>]";

    public static void start(String[] args) {
        CommandLineParser cp = new CommandLineParser(args);
        try {
            cp.parse();
            String savegameFileName = cp.getGameFilename();
            int usePort;
            if (cp.getPort() != -1) {
                usePort = cp.getPort();
            } else {
                usePort = PreferenceManager.getClientPreferences()
                        .getLastServerPort();
            }

            // kick off a RNG check
            megamek.common.Compute.d6();
            // start server
            Server dedicated;
            try {
                dedicated = new Server(PreferenceManager.getClientPreferences()
                        .getLastServerPass(), usePort);
            } catch (IOException ex) {
                StringBuffer error = new StringBuffer();
                error.append("Error: could not start server at localhost")
                        .append(":").append(usePort).append(" (").append(
                                ex.getMessage()).append(").");
                System.err.println(error.toString());
                return;
            }
            if (null != savegameFileName) {
                dedicated.loadGame(new File(savegameFileName));
            }
            return;
        } catch (AbstractCommandLineParser.ParseException e) {
            StringBuffer message = new StringBuffer(INCORRECT_ARGUMENTS_MESSAGE)
                    .append(e.getMessage()).append('\n');
            message.append(ARGUMENTS_DESCRIPTION_MESSAGE);
            displayMessage(message.toString());
        }
    }

    public static void main(String[] args) {
        start(args);
    }

    private static void displayMessage(String message) {
        System.out.println(message);
        System.out.flush();
    }

    private static class CommandLineParser extends AbstractCommandLineParser {

        private String gameFilename;
        private int port;

        // Options
        private static final String OPTION_PORT = "port"; //$NON-NLS-1$

        public CommandLineParser(String[] args) {
            super(args);
        }

        /**
         * Returns the port option value or <code>-1</code> if it wasn't set
         * 
         * @return port option value or <code>-1</code> if it wasn't set
         */
        public int getPort() {
            return port;
        }

        /**
         * Returns the game file name option value or <code>null</code> if it
         * wasn't set
         * 
         * @return the game file name option value or <code>null</code> if it
         *         wasn't set
         */
        public String getGameFilename() {
            return gameFilename;
        }

        protected void start() throws ParseException {
            if (getToken() == TOK_OPTION && getTokenValue().equals(OPTION_PORT)) {
                nextToken();
                parsePort();
            }
            if (getToken() == TOK_LITERAL) {
                gameFilename = getTokenValue();
                nextToken();
            }
            if (getToken() != TOK_EOF) {
                error("unexpected input"); //$NON-NLS-1$
            }
        }

        private void parsePort() throws ParseException {
            if (getToken() == TOK_LITERAL) {
                int port = -1;
                try {
                    port = Integer.decode(getTokenValue()).intValue();
                } catch (NumberFormatException e) {
                }
                nextToken();
                if (port < 0 || port > 65535) {
                    error("invalid port number"); //$NON-NLS-1$                                    
                }
                this.port = port;
            } else {
                error("port number expected"); //$NON-NLS-1$                
            }
        }

    }

}
