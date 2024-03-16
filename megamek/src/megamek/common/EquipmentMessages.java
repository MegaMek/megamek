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
package megamek.common;

import megamek.MegaMek;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class EquipmentMessages {
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("megamek.common.equipmentmessages",
            MegaMek.getMMOptions().getLocale());

    private EquipmentMessages() {

    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    /**
     * Returns the formatted message for the given key in the resource bundle.
     * 
     * @param key the resource name
     * @param args the message arguments
     * @return the string
     */
    public static String getString(String key, Object... args) {
        String s = getString(key);
        if (s != null) {
            return MessageFormat.format(getString(key), args);
        }
        return null;
    }
}
