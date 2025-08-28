/*
  Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.server;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import megamek.common.commandLine.AbstractCommandLineParser;
import megamek.common.commandLine.ClientServerCommandLineParser;
import megamek.common.commandLine.MegaMekCommandLineFlag;
import megamek.common.compute.Compute;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.EmailService;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

public class DedicatedServer {
    private static final MMLogger logger = MMLogger.create(DedicatedServer.class);

    public static void start(String[] args) {
        ClientServerCommandLineParser parser = new ClientServerCommandLineParser(args,
              MegaMekCommandLineFlag.DEDICATED.toString(),
              true, false, false);
        try {
            parser.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            logger.error("Incorrect arguments:" + e.getMessage() + '\n' + parser.help());
        }

        ClientServerCommandLineParser.Resolver resolver = parser.getResolver(
              PreferenceManager.getClientPreferences().getLastServerPass(),
              PreferenceManager.getClientPreferences().getLastServerPort(),
              null, null);

        EmailService mailer = null;
        if (resolver.mailPropertiesFile != null) {
            File propsFile = new File(parser.getMailProperties());
            try (var propsReader = new FileReader(propsFile)) {
                var mailProperties = new Properties();
                mailProperties.load(propsReader);
                mailer = new EmailService(mailProperties);
            } catch (Exception ex) {
                logger.error(
                      "Error: could not load mail properties file \"" +
                            propsFile.getAbsolutePath() + "\"",
                      ex);
                return;
            }
        }

        // kick off a RNG check
        Compute.d6();

        // start server
        Server server;

        try {
            server = new Server(resolver.password, resolver.port, new TWGameManager(), resolver.registerServer,
                  resolver.announceUrl, mailer, true);
        } catch (Exception ex) {
            logger.error("Error: could not start server at localhost" + ":" + resolver.port, ex);
            return;
        }

        File gameFile = resolver.getSaveGameFile();
        if (null != gameFile) {
            server.loadGame(gameFile);
        }
    }
}
