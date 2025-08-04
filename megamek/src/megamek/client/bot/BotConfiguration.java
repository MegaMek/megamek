/*
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import megamek.logging.MMLogger;

public class BotConfiguration {
    private final static MMLogger logger = MMLogger.create(BotConfiguration.class);
    static Properties BotProperties = new Properties();

    static {
        try (InputStream is = new FileInputStream("mmconf/bot.properties")) {
            BotProperties.load(is);
        } catch (Exception e) {
            logger.error(e, "Bot properties could not be loaded, will use defaults");
        }
    }

    public int getIgnoreLevel() {
        int difficulty = 3;
        try {
            difficulty = Integer.parseInt(BotProperties.getProperty("difficulty", "3"));
        } catch (Exception e) {
            // do nothing
        }

        switch (difficulty) {
            case 1:
                return 8;
            case 2:
                return 9;
            default:
                return 10;
        }
    }

    public boolean isForcedIndividual() {
        boolean forced = false;
        try {
            forced = Boolean.parseBoolean(BotProperties.getProperty(
                  "forceIndividualInitiative", "false"));
        } catch (Exception e) {
            // do nothing
        }
        return forced;
    }

    public boolean isDebug() {
        try {
            return Boolean.parseBoolean(BotProperties.getProperty("Debug", "false"));
        } catch (Exception e) {
            return false;
        }
    }
}
