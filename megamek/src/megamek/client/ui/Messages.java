/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
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

package megamek.client.ui;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import megamek.common.preference.PreferenceManager;
import megamek.common.util.EncodeControl;

public class Messages {
    private static final String BUNDLE_NAME = "megamek.client.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME,
            PreferenceManager.getClientPreferences().getLocale(), new EncodeControl());

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
            System.out.println("Missing i18n entry: " + key);
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
}