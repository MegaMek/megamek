/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import java.util.ResourceBundle;

import megamek.MegaMek;

public class BugReportMessages {

    private static final String BUNDLE_NAME = "megamek.client.BugReport";
    private static final ResourceBundle RESOURCE_BUNDLE =
          ResourceBundle.getBundle(BUNDLE_NAME, MegaMek.getMMOptions().getLocale());

    /**
     * Retrieves the string for the given key from this resource bundle or one of its parents. Additional parameters are
     * applied using MessageFormat (so the resource string should use {x} formatting). This method works without
     * giving additional parameters for strings that don't contain placeholders. Note that all exceptions are caught
     * and, in when one occurs, "!!! key !!!" is returned.
     *
     * @param key  The resource key
     * @param args Additional info to insert for placeholders
     *
     * @return The formatted resource bundle i18n string
     */
    public String get(String key, Object... args) {
        try {
            String message = RESOURCE_BUNDLE.getString(key);
            if (args.length == 0) {
                // avoid mangling of apostrophs when MessageFormat is unnecessary
                return message;
            }
            return MessageFormat.format(message, args);
        } catch (Exception ex) {
            return "!!! %s !!!".formatted(key);
        }
    }
}
