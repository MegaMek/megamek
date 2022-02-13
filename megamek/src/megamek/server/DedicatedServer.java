/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server;

import megamek.common.preference.PreferenceManager;
import megamek.common.util.AbstractCommandLineParser;
import megamek.common.util.EmailService;
import megamek.common.util.ServerCommandLineParser;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class DedicatedServer {
    private static final String INCORRECT_ARGUMENTS_MESSAGE = "Incorrect arguments:";
    private static final String ARGUMENTS_DESCRIPTION_MESSAGE = "Arguments syntax:\n\t "
            + "[-password <pass>] [-port <port>] [-mail <javamail.properties>] [<saved game>]";

    public static void start(String[] args) {
        ServerCommandLineParser cp = new ServerCommandLineParser(args);
        try {
            cp.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            LogManager.getLogger().error(INCORRECT_ARGUMENTS_MESSAGE + e.getMessage() + '\n'
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
                LogManager.getLogger().error(
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
            dedicated = new Server(password, usePort, !announceUrl.isBlank(), announceUrl, mailer, true);
        } catch (Exception ex) {
            LogManager.getLogger().error("Error: could not start server at localhost" + ":" + usePort, ex);
            return;
        }
        if (null != saveGameFileName) {
            dedicated.loadGame(new File(saveGameFileName));
        }
    }

    public static void main(String[] args) {
        start(args);
    }

}
