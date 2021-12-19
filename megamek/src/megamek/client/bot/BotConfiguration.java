/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot;

import org.apache.logging.log4j.LogManager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class BotConfiguration {

    static Properties BotProperties = new Properties();

    static {
        try (InputStream is = new FileInputStream("mmconf/bot.properties")) {
            BotProperties.load(is);
        } catch (Exception e) {
            LogManager.getLogger().error("Bot properties could not be loaded, will use defaults", e);
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
