/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
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
package megamek.client.ui;

import megamek.MegaMek;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("megamek.client.messages",
            MegaMek.getMMOptions().getLocale());

    // All static class, should never be instantiated
    private Messages() { }

    /** Check to see if a given key has valid internationalized text. */
    public static boolean keyExists(String key) {
        return RESOURCE_BUNDLE.containsKey(key);
    }

    /** Returns the internationalized text for the given key in the resource bundle. */
    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            LogManager.getLogger().error("Missing i18n entry: " + key);
            return '!' + key + '!';
        }
    }

    /**
     * Returns the formatted internationalized text for the given key in the resource bundle,
     * replacing occurrences of {x} in the message with the contents of args.
     */
    public static String getString(String key, Object... args) {
        return MessageFormat.format(getString(key), args);
    }

    /**
     * Returns the formatted internationalized text for the given key in the resource bundle,
     * replacing occurrences of %s in the message with the contents of args.
     *
     * This is ONLY to be used for string that need to be formatted without formatting the number
     */
    public static String getFormattedString(String key, Object... args) {
        return String.format(getString(key), args);
    }
}