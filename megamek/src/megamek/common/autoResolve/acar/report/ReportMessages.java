/*
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

package megamek.common.autoResolve.acar.report;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import megamek.MegaMek;

public class ReportMessages {

    private static final ResourceBundle RESOURCE_BUNDLE =
          ResourceBundle.getBundle("megamek.client.acs-report-messages", MegaMek.getMMOptions().getLocale());

    private static final ResourceBundle EN_RESOURCE_BUNDLE =
          ResourceBundle.getBundle("megamek.client.acs-report-messages", Locale.ENGLISH);

    private static String getEnString(String key) {
        if (EN_RESOURCE_BUNDLE.containsKey(key)) {
            return EN_RESOURCE_BUNDLE.getString(key);
        } else {
            return null;
        }
    }

    public static String getString(String key) {
        if (RESOURCE_BUNDLE.containsKey(key)) {
            return RESOURCE_BUNDLE.getString(key);
        } else {
            return getEnString(key);
        }
    }

    /**
     * Returns the formatted message for the given key in the resource bundle.
     *
     * @param key  the resource name
     * @param args the message arguments
     *
     * @return the string
     */
    public static String getString(String key, Object... args) {
        try {
            String message = getString(key);
            return MessageFormat.format(message, args);
        } catch (NullPointerException ex) {
            return "[Missing report message " + key + "]";
        } catch (IllegalArgumentException exception) {
            return "[Invalid message data for " + key + "]";
        }
    }

    /**
     * Returns the formatted message for the given key in the resource bundle.
     *
     * @param key  the resource name
     * @param data the message arguments
     *
     * @return the string
     */
    public static String getString(String key, List<Object> data) {
        return getString(key, data.toArray());
    }

    private ReportMessages() {}
}
