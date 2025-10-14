/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import megamek.MegaMek;

public class Messages {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("megamek.client.messages",
          MegaMek.getMMOptions().getLocale());

    // All static class, should never be instantiated
    private Messages() {
    }

    /** Check to see if a given key has valid internationalized text. */
    public static boolean keyExists(String key) {
        return RESOURCE_BUNDLE.containsKey(key);
    }

    /**
     * Returns the internationalized text for the given key in the resource bundle.
     */
    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /**
     * Returns the formatted internationalized text for the given key in the resource bundle, replacing occurrences of
     * {x} in the message with the contents of args.
     */
    public static String getString(String key, Object... args) {
        return MessageFormat.format(getString(key), args);
    }

    /**
     * Returns the formatted internationalized text for the given key in the resource bundle, replacing occurrences of
     * %s in the message with the contents of args.
     * <p>
     * This is ONLY to be used for string that need to be formatted without formatting the number
     */
    public static String getFormattedString(String key, Object... args) {
        return String.format(getString(key), args);
    }
}
