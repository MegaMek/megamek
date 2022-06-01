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

import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.common.commandline.AbstractCommandLineParser;
import megamek.common.commandline.ClientServerCommandLineParser;
import megamek.common.commandline.MegaMekCommandLineFlag;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.EmailService;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class DedicatedServer {

    public static void start(String[] args) {
        ClientServerCommandLineParser parser = new ClientServerCommandLineParser(args,
                MegaMekCommandLineFlag.DEDICATED.toString(),
                true, false, false);
        try {
            parser.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            LogManager.getLogger().error(parser.formatErrorMessage(e));
        }

        ClientServerCommandLineParser.Resolver resolver = parser.getResolver(
                PreferenceManager.getClientPreferences().getLastServerPass(),
                PreferenceManager.getClientPreferences().getLastServerPort(),
                null, null
                );

        EmailService mailer = null;
        if (resolver.mailPropertiesFile != null) {
            File propsFile = new File(parser.getMailProperties());
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
        Server server;

        try {
            server = new Server(resolver.password, resolver.port, new GameManager(), resolver.registerServer, resolver.announceUrl, mailer, true);
            MegaMek.printToOut(Messages.getFormattedString("MegaMek.ServerStarted", server.getHost(), server.getPort(), server.isPassworded() ? "enabled" : "disabled") + "\n");
        } catch (Exception ex) {
            LogManager.getLogger().error("Error: could not start server at localhost" + ":" + resolver.port, ex);
            MegaMek.printToOut(Messages.getFormattedString("MegaMek.ServerStartFailed"));
            MegaMek.printToOut(ex.getLocalizedMessage());
            return;
        }

        if (null != resolver.saveGameFileName) {
            server.loadGame(new File(resolver.saveGameFileName));
        }
    }

    public static void main(String[] args) {
        start(args);
    }

}
