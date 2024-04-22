/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.scenario;

import megamek.MegaMek;
import org.apache.logging.log4j.LogManager;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(
            "megamek.client.scenario.messages", MegaMek.getMMOptions().getLocale());

    private Messages() { }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            LogManager.getLogger().error("Missing megamek.client.scenario.messages i18n entry with key " + key);
            return '!' + key + '!';
        }
    }
}